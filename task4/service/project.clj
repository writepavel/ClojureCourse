;;(
 (defproject service "0.1.0-SNAPSHOT"
   :description "FIXME: write description"
   :url "http://example.com/FIXME"
   :license {:name "Eclipse Public License"
             :url "http://www.eclipse.org/legal/epl-v10.html"}
   :dependencies [[org.clojure/clojure "1.6.0"]
                  [compojure "1.1.6"]
                  [sonian/carica "1.1.0" :exclusions [[cheshire]]]
                  [korma "0.3.1"]
                  [hiccup "1.0.5"]
                  [clj-tagsoup "0.3.0"]
                  [clj-http "0.9.2"] ;; Чтобы делать самостоятельные запросы в evernote.
                  ;;[http-kit "2.1.16"]
                  ;;[ring/ring-devel "1.3.0-RC1"]
                  ;;[ring/ring-core "1.3.0-RC1"]
                  [mysql/mysql-connector-java "5.1.30"]
                  [fogus/ring-edn "0.2.0"]
                  [clojurenote "0.4.0"]]
   :plugins [[lein-ring "0.8.10"]]
   :ring {:handler service.core/app} ;; Переехал на http-kit чтобы делать самостоятельные запросы в evernote
  ;; :main service.core
   :resource-paths ["resources" "../client/resources"])
