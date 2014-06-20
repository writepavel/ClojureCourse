(ns service.core
  (:require [compojure.route :as route]
                 [clojure.java.io :as io]
                 [ring.util.response :as resp]
                 [clj-http.client :as client]
                 [clojurenote.auth :as auth])
  (:use compojure.core
;;        org.httpkit.server :only [run-server]
        compojure.handler
        ring.middleware.edn
        carica.core
        service.evernote_client
        service.helpers
;;        korma.db
  ;;      korma.core
        ))

;; (defdb db {:classname "com.mysql.jdbc.Driver"
;;            :subprotocol "mysql"
;;            :user (config :db :user)
;;            :password (config :db :pass)
;;            :subname (str "//" (get env "OPENSHIFT_MYSQL_DB_HOST") ":" (get env "OPENSHIFT_MYSQL_DB_PORT") "/" (config :db :name) "?useUnicode=true&characterEncoding=utf8")
;;            :delimiters "`"})

;;(defentity article)


(def app-state (atom {:from-server "HELLO!"}))
(def evernote-login-status (atom false))


(defn show-main-page []
  (slurp (io/resource "public/index.html")))

;;-----------------------------start----------------------------------------------------------
;; Copyed from clojurenote-demo
;; https://github.com/mikebroberts/clojurenote
;;----------------------------------------------------------------------------------------------


;;(def host "http://localhost:3000")
;; (def host "http://thinkhabit.ru")
(def host (str "http://" (get env "thinkhabit_host")))

(defn login-evernote
  "Obtain request token, put it in session, and redirect to the URL we're given"
  []
  (println "LOGIN_EVERNOTE")
  (let [{url :url :as request-token} (auth/obtain-request-token (evernote-config))]
      (-> url
          (resp/redirect)
          (assoc-in [:session :request-token] request-token))))

(defn on-successful-login [t my-host]
  (reset! evernote-login-status true)
  (reset! en-user {:access-token (:access-token t) :notestore-url (:notestore-url t)})
  (init-notebook-guid)
  ;;(println "LOGIN SUCCESSFUL! app-state = " @app-state " evernote-login-status = " evernote-login-status ", en-user = " @en-user)
  (resp/redirect my-host)
  ;;(show-main-page)
 ;;  (str "<html><body>
;;           <p>Successfully logged into Evernote.</p>
;;           <p>User access token is " (:access-token t) "</p>
;;           <p>User notestore URL is " (:notestore-url t) "</p>
;;           <p>Copy the access token and notestore URL, then try using the
;;             api <a href='/use'>here</a>.</p>
;;           <p>Full user details are as follows:</p>
;;           <code>" t "</code>
;; <p></p>
;; <p>app-state = " @app-state  "</p>
;;           </body></html>"
;;    )
  )


(defn evernote-oauth-callback
  "Given a verifier code from URL, and request token from session, obtain access token.
    Check that there's been no token weirdness by comparing oauth-token on URL
    with request token in session"
  [{:keys [oauth_verifier oauth_token] :as params} {:keys [request-token]}]
   (println "evernote-oauth-callback")
  (if (= oauth_token (:token request-token))
    (->
      (auth/obtain-access-token (evernote-config) oauth_verifier request-token)
      (on-successful-login (str "http://" (get env "thinkhabit_host"))))
    (throw (Exception.
      "ERROR - OAuth token on callback, and request token in session, did not match"))
    ))



;;--------------------------finish-------------------------------------------------------



;; (defn proxy-request [ajax-request]
;;   )

(defroutes app-routes
  (GET "/state" []
       (println "sending app-state = " @app-state ", full response is " (response @app-state))
       (response @app-state))
  (POST "/state" {:keys [params]}
         (swap! app-state merge  (:app-state params))))

;;(defroutes questionkit-routes)

(defroutes login-routes
  (GET "/evernote" [] (login-evernote))
  (POST "/evernote" [] (login-evernote))
  (GET   "/evernote-status" [] (response @evernote-login-status))
  (POST "/evernote-status" request
        ;;(println "Received new evernote-login-status. = " (:login-status (:params request)))
        (reset! evernote-login-status (:login-status (:params request)))
        (response @evernote-login-status))
  (GET   "/evernote-user" [] (response @en-user))
  (POST "/evernote-user" request
        ;;(println "Received new evernote-login-status. = " (:login-status (:params request)))
        (reset! en-user (:en-user (:params request)))
        (reset! evernote-login-status (not-empty @en-user))
        (response @evernote-login-status)))


;; TODO use comp
(defn- response-qklist []
;;                 (println "invoked responce-qklist. Response = " (resp/charset (response (questionkit-list)) "UTF-8"))
                 (resp/charset (response (questionkit-list)) "UTF-8"))



(defroutes compojure-handler
  (GET "/" [] (show-main-page))
  (GET "/req" request (str request "\n \n host env = " (get env "thinkhabit_host")))
  (GET "/app/state" [] (response @app-state))
  ;;  (
   (GET "/questionkit-list" [] (response-qklist))
   (POST "/questionkit-list" {:keys [params]}
         (try-update-questionkit (:note-map params))
         (response-qklist))
   (POST "/answers" {:keys [params]}
         (try-send-answers (:answers params))
         (response nil))
   (POST "/app/state" {:keys [params]}
         (swap! app-state merge (:app-state params)))
   ;; (context "/app" app-routes)
   ;;  (context "/questionkit" [] questionkit-routes)
   (context "/login" [] login-routes)
   (GET "/evernote-oauth-callback" {:keys [params session]}
        (evernote-oauth-callback params session))
   (route/resources "/")
   (route/files "/" {:root (config :external-resources)})
   (route/not-found "Not found!"))

(def app
  (-> compojure-handler
      site
      wrap-edn-params))
