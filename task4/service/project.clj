 (defproject service "0.1.1-SNAPSHOT"
   :description "ThinkHabit.ru - Service for gathering of thoughts"
   :url "https://thinkhabit.ru"
   :license {:name "Eclipse Public License"
             :url "http://www.eclipse.org/legal/epl-v10.html"}
   :dependencies [[org.clojure/clojure "1.8.0"]
                  [compojure "1.6.0"]
                  [sonian/carica "1.2.2" :exclusions [[cheshire]]]
                  [korma "0.4.3"]
                  [hiccup "1.0.5"]
                  [clj-tagsoup "0.3.0" :exclusions [org.clojure/clojure]]
                  [clj-http "3.7.0"] ;; Чтобы делать самостоятельные запросы в evernote.
                  ;;[http-kit "2.1.16"]
                  ;; [mysql/mysql-connector-java "5.1.30"]
                  [fogus/ring-edn "0.3.0"]
                  [clojurenote "0.4.0"]]
   :plugins [[lein-ring "0.12.3"]]
   :ring {:handler service.core/app}
   :main service.core
   :resource-paths ["resources" "../client/resources"])
