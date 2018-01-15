(ns service.core
  (:require [compojure.route :as route]
                 [clojure.java.io :as io]
                 [ring.util.response :as resp]
                 [clj-http.client :as client]
                 [clojurenote.auth :as auth]
                 [ring.middleware.session.memory :as mem])
  (:use compojure.core
;;        org.httpkit.server :only [run-server]
        compojure.handler
        ring.middleware.edn
        ring.middleware.session
        carica.core
        service.evernote_client
        service.helpers
;;        korma.db
  ;;      korma.core
        )
  (:import java.util.UUID))

;; (defdb db {:classname "com.mysql.jdbc.Driver"
;;            :subprotocol "mysql"
;;            :user (config :db :user)
;;            :password (config :db :pass)
;;            :subname (str "//" (get env "OPENSHIFT_MYSQL_DB_HOST") ":" (get env "OPENSHIFT_MYSQL_DB_PORT") "/" (config :db :name) "?useUnicode=true&characterEncoding=utf8")
;;            :delimiters "`"})

;;(defentity article)


(def app-state (atom {:from-server "HELLO!"}))
;; Save this info in atom with session_id or in session on client side. When there is no adequate securing with ssl, this will be saved on server side.
;;
(def evernote-login-status (atom {"sessionid" false}))
;;(def evernote-login-status (atom false))


;; (defn tec []
;;   (pprint (last @en-sessions )))

;; (tec)

(defn show-main-page []
  (slurp (io/resource "public/index.html")))

;;-----------------------------start----------------------------------------------------------
;; Copyed from clojurenote-demo
;; https://github.com/mikebroberts/clojurenote
;;----------------------------------------------------------------------------------------------


(def host (str "http://" (get env "thinkhabit_host")))

(defn login-evernote
  "Obtain request token, put it in session, and redirect to the URL we're given"
  []
  (println "LOGIN_EVERNOTE")
  (let [{url :url :as request-token} (auth/obtain-request-token (evernote-config))]
    (-> url
        (str "")
          (resp/redirect)
          (assoc-in [:session :request-token] request-token))))

(defn on-successful-login [t sessionid my-host]
;;  (println "\n\nSucessful login. sessionid = " sessionid ", t = " t ", my-host = " my-host)
  (swap! evernote-login-status assoc-in [sessionid] true)
  (set-data sessionid :en-user {:access-token (:access-token t) :notestore-url (:notestore-url t)})
  (init-notebook-guid sessionid)
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


(defn ___on-successful-login [sessionid t my-host]
  (swap! evernote-login-status update-in [sessionid] true)
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


;; TODO extract sessionid fron ring-session parameter and send it to evernote as callback-parameter

(defn evernote-oauth-callback
  "Given a verifier code from URL, and request token from session, obtain access token.
    Check that there's been no token weirdness by comparing oauth-token on URL
    with request token in session"
  [sessionid {:keys [oauth_verifier oauth_token] :as params} {:keys [request-token]}]
   ;;(println " \n\n!! evernote-oauth-callback. Sessionid = " sessionid)
  (if (= oauth_token (:token request-token))
    (->
      (auth/obtain-access-token (evernote-config) oauth_verifier request-token)
      (on-successful-login sessionid (str "http://" (get env "thinkhabit_host"))))
    (throw (Exception.
      "ERROR - OAuth token on callback, and request token in session, did not match"))
    ))

(defn- session [request]
  (let [sid (get-in request [:cookies "ring-session" :value])]
    (if sid sid (UUID/randomUUID))))

(defn- session-cookie [cookies]
  (let [sid (get-in cookies ["ring-session" :value])]
    (if sid sid (UUID/randomUUID))))


(defn receive-evernote-user-data [request]
 ;;       (println "\n\n!!==========!!\n\nReceived EN-DATA. en-user. = " (:en-user (:params request)))
        (set-data (session request) :en-user (:en-user (:params request)))
;;        (println "\n\n!!==========!!\n\nReceived EN-DATA. evernote-login-status current = " @evernote-login-status ", status from request = " (not (empty? (get-data (session request) :en-user))))
        (swap! evernote-login-status assoc-in [(session request)] (not (empty? (get-data (session request) :en-user))))
;;        (println "\n\n!!==========!!\n\nReceived EN-DATA. AFTER UPDATING evernote-login-status = " @evernote-login-status)
        (response (get-in @evernote-login-status [(:session request)])))

;;--------------------------finish-------------------------------------------------------



;; (defn proxy-request [ajax-request]
;;   )

(defroutes app-routes
  (GET "/state" []
 ;;      (println "sending app-state = " @app-state ", full response is " (response @app-state))
       (response @app-state))
  (POST "/state" {:keys [params]}
         (swap! app-state merge  (:app-state params))))

;;(defroutes questionkit-routes)

(defroutes login-routes
  (GET "/evernote" [] (login-evernote))
  (POST "/evernote" [] (login-evernote))
  (GET   "/evernote-status" request (response (get-in @evernote-login-status (:session request))))
  (POST "/evernote-status" request
        ;;(println "Received new evernote-login-status. = " (:login-status (:params request)))
        (swap! evernote-login-status assoc-in [(:session request)] (:login-status (:params request)))
        (response (get-in @evernote-login-status (:session request))))
  (GET   "/evernote-user" request (response (get-data (session request) :en-user)))
  (POST "/evernote-user" request (receive-evernote-user-data request)))


;; TODO use comp
(defn- response-qklist [sessionid]
;;                 (println "invoked responce-qklist. Response = " (resp/charset (response (questionkit-list)) "UTF-8"))
                 (resp/charset (response (questionkit-list sessionid)) "UTF-8"))

(defroutes compojure-handler
  (GET "/" [] (show-main-page))
  
  (GET "/req" request (str request "\n \n host env = " (get env "thinkhabit_host") "\n\n session contents is " (session request)))
  
  (GET "/app/state" [] (response @app-state))
  ;;  (
  
   (GET "/questionkit-list" request (response-qklist (session request)))
  
   (POST "/questionkit-list" {:keys [params cookies]}
         (try-update-questionkit (session-cookie cookies) (last (:note-map params)))
         (response-qklist (session-cookie cookies)))
  
   ;; (POST "/save-new-qks" {:keys [params cookies]}
   ;;      (try-send-new-qks))
  
   (POST "/answers" {:keys [params cookies]}
         (try-send-answers (session-cookie cookies) (:answers params))
         (response nil))
  
   (POST "/app/state" {:keys [params]}
         (swap! app-state merge (:app-state params)))
   ;; (context "/app" app-routes)
   ;;  (context "/questionkit" [] questionkit-routes)

  (context "/login" [] login-routes)

  (GET "/evernote-oauth-callback" {:keys [params session cookies]}
;;        (println "(GET /evernote-oauth-callback. params =  " params)
        (evernote-oauth-callback (session-cookie cookies) params session))

  (route/resources "/")
  (route/files "/" {:root (config :external-resources)})
  (route/not-found "Not found!"))

(def app
  (-> compojure-handler
      site
      wrap-edn-params))
