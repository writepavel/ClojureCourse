(ns service.helpers
 ;;  (:require [compojure.route :as route]
;;                  [clojure.java.io :as io]
;;                  [ring.util.response :as resp]
;;                  [clj-http.client :as client]
;; ;;                 [ring.middleware.reload :as reload]
;;                  [clojurenote.auth :as auth])
;;   (:use compojure.core
;; ;;        org.httpkit.server :only [run-server]
;;         compojure.handler
;;         ring.middleware.edn
;; ;;        clojurenote.notes
;;   ;;      clojurenote.enml
;;         carica.core
;;    ;;     service.evernote_client
;; ;;        korma.db
;;   ;;      korma.core        )
  )


(defn response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})


(def env (into {} (System/getenv)))

(defn log [message & strs]
  (when true (println (str message strs))))

