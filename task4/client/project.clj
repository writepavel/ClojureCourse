(defproject client "0.1.1-SNAPSHOT"

  :dependencies [
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [enfocus "2.1.1"] ;; Dommy loads automatically  [prismatic/dommy "0.1.2"]
                 [prismatic/dommy "0.1.2"]
                 [jayq "2.5.1"]
                 [tongue "0.2.3"]
             ;;    [hickory "0.5.3"] ;; for parsing evernote notes
                 [shoreleave/shoreleave-browser "0.3.0"]
                 [cljs-ajax "0.2.3"]]
  :profiles {:dev {:plugins [[lein-cljsbuild "1.1.7"]]}}
  :cljsbuild {
              :repl-listen-port 9000
              :repl-launch-commands
               {"my-launch" ["C:\\Program Files\\Mozilla Firefox\\firefox.exe" "-jsconsole" "http://localhost:3000"]
              ;; "my-other-launch" ["firefox" "-jsconsole"]
               "linux-launch" ["/usr/bin/firefox" "-jsconsole" "http://localhost:3000"]
               "my-other-launch" ["firefox" "-jsconsole"]}
               :builds [{
                         :source-paths ["src"]
                         :compiler {
                                    :output-to "resources/public/js/main.js"
                                    :externs [
                                              "resources/public/js/bootstrap-wysihtml5.js"
                                              "resources/public/js/jquery-1.9.js"
                                              ;;"resources/public/js/evernote-sdk-minified.js"
                                             ;; "resources/public/js/jsOAuth-1.3.7.min.js"
                                              ]
                                    ;; :libs ["resources/jslib/"]
                                    ;; :foreign-libs [
                                    ;;                {:file "resources/public/js/evernote-sdk-minified.js"
                                    ;;                 :provides ["evernote"]}
                                    ;;                {:file "resources/public/js/jsOAuth-1.3.7.min.js"
                                    ;;                 :provides ["oauth"]}]
                                    :optimizations :whitespace
                                    ;; :optimizations :advanced
                                    :pretty-print true}}]})
