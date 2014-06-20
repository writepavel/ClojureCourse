(ns client.helpers
  (:require [enfocus.core :as ef]
                [shoreleave.browser.storage.sessionstorage :as sessionstorage]
                [ajax.core :refer [GET POST]]
                ;; [evernote :as evernote]
                 ;;[oauth :as oauth]
                 )
  (:require-macros [enfocus.macros :as em]))

(defn log [message & strs]
   (when false (.log js/console (str message strs))))


(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "something bad happened: " status " " status-text)))
