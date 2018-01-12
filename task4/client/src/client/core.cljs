(ns client.core
  (:require [enfocus.core :as ef]
                [enfocus.events :as events]
                [enfocus.bind :as bind]
                [shoreleave.browser.storage.sessionstorage :as sessionstorage]
                [shoreleave.browser.storage.localstorage :as localstorage]
                [ajax.core :refer [GET POST]]
                [client.questionkit :as qk]
                [jayq.core :as jq]
                [clojure.browser.repl :as repl]
                [client.helpers :refer [ log error-handler]])
  (:use [jayq.core :only [$]])
  (:require-macros [enfocus.macros :as em]))

;;(repl/connect "http://localhost:9000/repl")
;;(log "repl connect. Repl = " repl)
(def questionkit-list (atom []))

;; (em/defaction change [msg]
;;   ["#button1"] (ef/content msg))

;; (em/defaction setup-listen []
;;   ["#button1"] (events/listen :click #(change "I have been clicked")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;   RENDER START PAGE   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(em/defsnippet brandlink "template-index.html" "a.navbar-brand" []
  ["a.navbar-brand"] (ef/do-> (ef/content "")
                     (ef/remove-attr :href)
                     (ef/set-attr :href "#" :onclick "client.core.render_page()")))

(em/defsnippet site-header "template-index.html" "header" []
  ["ul.nav"] (ef/do-> (ef/content "")
                                (ef/append (ef/html [:li [:a {:href "#" :onclick "client.core.login_by_evernote()"} "Войти через свой Evernote "]]))
                                (ef/append (ef/html [:li [:a {:href  "#" :id "button1"
                                                              :onclick "client.core.evernote_gift()"
                                                              } "У меня ещё нет Evernote и хочу подарок"]]))
                                )
    ["a.navbar-brand"] (ef/do->;; (ef/content "")
                     (ef/remove-attr :href)
                     (ef/set-attr :href "#" :onclick "client.core.render_page()"))
  ;; ["a.navbar-brand"] (ef/do->
  ;;                    (ef/remove-attr :href)
  ;;                    (ef/set-attr :href "#" :onclick "client.core.render_page()"))
                         )

(em/defsnippet site-slider "template-index.html" "#main-slider" []
   ["div.embed-container"] (ef/content "")
   ["div.carousel-content"] (ef/content "")
   ["div.carousel"] (ef/set-attr :id "main-carousel")
   ["ol li"] (ef/set-attr :data-target "#main-carousel")
   ["a.hidden-xs"] (ef/set-attr :href "#main-carousel"))

;; http://www.plugolabs.com/twitter-bootstrap-button-generator-3/

(em/defsnippet site-services "template-index.html" "#services" []

  ;; http://www.minimit.com/articles/solutions-tutorials/bootstrap-3-responsive-columns-of-same-height
  ;; [".container"] (ef/add-class "container-sm-height")
  ;; [".row"] (ef/add-class "row-sm-height")
  ;; [".col-md-6"] (ef/add-class "col-sm-height")
  ;; [".col-md-6"] (ef/add-class "col-bottom")

  ["#signin-block .media-heading"] (ef/content  "Войти через свой аккаунт Evernote" )

  ["#signin-block .media-body p"] (ef/content (ef/html
                                               [:div {:align "center" :style "vertical-align:bottom;"}
                                                 [:a.btn.btn-default.btn-danger.btn-lg
                                                 {:href "#" :onclick "client.core.login_by_evernote()"}
                                                 "Войти     "  
                                                 [:span.glyphicon.glyphicon-log-in]]]))

  ["#gift-block .media-heading"] (ef/content "У меня ещё нет Evernote и хочу подарок")

  ["#gift-block .media-body p"] (ef/content (ef/html
                                             [:div {:align "center"}
                                             [:a.btn.btn-default.btn-danger.btn-lg
                                               {:href "#" :onclick "client.core.evernote_gift()" }
                                               "Регистрация    "
                                               [:span.glyphicon.glyphicon-gift]]])))

(em/defsnippet site-recentworks "template-index.html" "#bottom" []
  ["#bottom"] (ef/content ""))

(em/defsnippet site-footer "template-index.html" "#footer" []
  ["footer"] (ef/content ""))

(em/defsnippet site-page-title "about-us.html" "#title" [title subtitle]
  ["h1"] (ef/content
          (ef/html [:br])
                     "\n\n"  title "\n\n"
            ;;         (ef/html [:br])
                     )
  ["p"] (ef/content subtitle)
  [".breadcrumb"] (ef/content ""))



(defn show-index []
  (ef/at ".container1"
          (ef/do-> (ef/content (site-header))
                        (ef/append (site-slider))
                        (ef/append (site-services))
                        (ef/append (site-recentworks))
                        (ef/append (site-footer)))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(em/defsnippet site-header-questions "template-index.html" "header" []
 ;; (site-header)
  ["ul.nav"] (ef/do-> (ef/content "")
                             ;;   (ef/append (ef/html [:li [:a {:href "#" :onclick "client.core.show_questionkits()"} "Вернуться к вопросам"]]))
                                (ef/append (ef/html [:li [:a {:href "#" :onclick "client.core.logout()"} "Выйти"]]))
                                ;; (ef/append (ef/html [:li [:a {:href "#" :onclick "client.core.evernote_gift()"} "У меня ещё нет Evernote и хочу подарок"]]))
                                 )
    ["a.navbar-brand"] (ef/do->;; (ef/content "")
                     (ef/remove-attr :href)
                     (ef/set-attr :href "#" :onclick "client.core.render_page()")))


;; (defn- join-array-elem [str1 str2]
;;   (.concat str1 joiner str2))

(defn- gather-launcher-params [qkdata]
  (let [str-elems (reduce (partial qk/join-with "', '") (:questions qkdata))]

  ;;(log "__TO_ARRAY___ ['" str-elems "']")
  (str "['" str-elems "']"))
  )

;; notebean (:title :content :guid) -> qk (:name :answer-name :questions :note-guid)

(em/defsnippet edit_questionkit "about-us.html" "#kit-editor" [{:keys [name answer-name questions]}]
  "#name" (ef/set-attr :value name)
  "#answer-name" (ef/set-attr :value answer-name)
  "#kit-questions-area" (ef/set-attr :value questions))

(em/defsnippet launcher "about-us.html" "#launcher" [qkdata]
  ["button"] (ef/do-> (ef/content (:name qkdata))
                      (ef/set-attr :style "margin-bottom: 20px;")
                      ;; (ef/set-attr :onclick "client.core.edit_questionkit(" ((comp qk/html->qk qk/qk->formdata) (:content notebean)) ")")

                      (ef/set-attr :onclick (str "client.core.show_answer_gameplay( '" (:name qkdata) "', '" (:answer-name qkdata) "', " (gather-launcher-params qkdata) ")" ))
                      )
  ;;(log "Launcher render! content of notebean is " (:questions qkdata))
  )

;; http://clojuredocs.org/clojure_core/clojure.core/contains_q
(defn- contains-name? [oldnames val]
  (log "__invoked contains-name? oldnames = " oldnames ", (:name val) = " (:name val) ", result = " (some #{(:name val)} oldnames ) )
  (some #{(:name val)} oldnames ))

(defn- insert-new-values-only [coll vals]
  (let [old-qk-names (mapv #(:name %) coll)
        new-only (remove (partial contains-name? old-qk-names) vals)]
    (log "NEW_VALS_INSERTER" ", old-qk-names is " old-qk-names ", new-only = " new-only)
    (into coll new-only)))

;; invokes by loading from evernote
(defn update-questionkit-list [notebean-list]
  (log "_UPDATE_ (map qk/note->qk notebean-list) = " (map qk/note->qk notebean-list))
  (swap! questionkit-list insert-new-values-only (map qk/note->qk notebean-list)))

;; invokes by add-watch, listener of changing questionkit atom
(defn render-questionkit-list [qklist]
  (log "invoke render-questionkit-list. Data = " qklist)
  (if qklist
    (do
      (ef/at "#questionkit-list" (ef/content (map launcher qklist)))
   ;;   (ef/at "#wait-message" (ef/content ""))
      )
    (ef/at "#wait-message" (ef/content "У вас ещё нет комплекта вопросов. Создайте его, заполнив форму справа."))))

(defn success-alert [data]
  (log "SUCCESS_ALERT. Data is cynced with server!"))

;; invokes by add-watch, listener of changing questionkit atom
(defn try-post-questionkit-list [qklist]
  (let [note-map (mapv #(qk/qk->note %) qklist)]

   ;; (log "try-post-questionkit. NoteMap = " note-map ". Generated from qklist = " qklist)
    (POST "questionkit-list"
        {:params {:note-map note-map}
         :handler success-alert
        :error-handler error-handler})))

(em/defsnippet questioneditor "about-us.html" "#kit-editor" []
  ["#new-kit-submit"] (ef/content "")
  ["#kit-questions-area"] (ef/set-attr :rows "7")
  ["#new-kit-link"] (ef/content "Добавить новый")
  ["#new-kit-link"] (ef/set-attr  :onclick "client.core.add_questionkit()")
;;  ["form"] (ef/set-attr :action "" :method "POST" :onsubmit "return client.core.try-add-questionkit()" )
  ;;["form"] (ef/remove-class "form-horizontal")
  )



(defn- clear-form []
  (ef/at "#kit-editor" (ef/content (questioneditor) )))



(defn ^:export add-questionkit []
  (let [formdata (ef/from "#kit-editor form"  (ef/read-form))
         qkdata (qk/formdata->qk formdata)]
    (swap! questionkit-list conj qkdata)
    (clear-form)
    (log "First element of questionkit-atom is" (first @questionkit-list))))

;; TODO where to put #wait-message content "" to save main message while (map launcher) works?

(em/defsnippet site-questionkits "about-us.html" "#about-us" []
  ["#team"] (ef/content "")
  ["#left-side h2"] (ef/content "Выберите комплект")
  ["#right-side h2"] (ef/content "Редактор")
  ["#right-content"] (ef/content (questioneditor))
  ["#left-content"] (ef/do-> (ef/content (ef/html [:div [:legend "подготовлено из заметок с тэгом  \"questionkit\""] [:h5#wait-message "Кнопки могут загружаться из Evernote в течение минуты"] [:div#questionkit-list]]))
                                        ;;    (ef/append (ef/html [:div#questionkit-list ]))
                             )
;;  ["#questionkit-list"] (ef/content (ef/html [:h3 "Если в вашем evernote есть уже комплекты вопросов, то они загрузятся сюда в течение минуты"]))
  )


(defn try-load-questionkits []
  (GET "/questionkit-list"
       {:handler update-questionkit-list
        :error-handler error-handler}))


(defn ^:export show-questionkits []
  (ef/at ".container1"
         (ef/do-> (ef/content (site-header-questions))
                       (ef/append (site-page-title "Комплекты вопросов" ""))
                       (ef/append (site-questionkits))
                       ;;(ef/append (site-footer))
                       )))




;;(def storage (sessionstorage/storage))
(def storage (localstorage/storage))

;;(def session-storage (sessionstorage/storage))

(def storage-available (.isAvailable storage))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;    RENDER ANSWER-GAMEPLAY  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;




(defn- is-null? [value]
;;  (log "_?_inviked is_null? (" value ") result is " (= (or nil "null" "nil" '(nil)) value) "ismap? " (map? value))
  (cond
           (map? value)  (do
  ;;                                     (log "value is map. vals are " (vals value))
                                       (reduce #(and %1 (is-null? %2)) true (vals value)))
           (coll? value) (do
    ;;                                (log "Collection. IS_EMPTY? is " (empty? value))
                                    (if (empty? value) true (is-null? (first value))))
           :else (= (or nil "null" "" "nil" '(nil)) value)))

(defn not-equal? [val1 val2]
  (and (not= val1 val2) (not (and (is-null? val1) (is-null? val2)))))

(defn- storage-has-element? [key-str]
  (let [value (.get storage key-str)]
    (and
     (not= nil (type value))
     (not (is-null? value)))))

;; (defn- site-header-answers  []
;;   (site-header-questions)
;;   (ef/at "ul.nav"
;;          (ef/do-> (ef/content "")
;;                                 (ef/append (ef/html [:li [:a {:href "#" :onclick "client.core.show_questionkits()"} "Вернуться к вопросам"]]))
;;                                 (ef/append (ef/html [:li [:a {:href "#" :onclick "client.core.logout()"} "Выйти"]])))))

;;
;; нажатие на кнопке "готово"
;;    - создается заметка с двумя тэгами "имя_списка_вопросов" и "group_by_time" и заголовком "заголовок_ответа"
;;    - ищется заметка с двумя тегами "имя_списка_вопросов" и "group_by_question" и заголовком "вопрос для ответа" . Если найдена, то добавляется ответ в конец. Если не найдена, то создается новая.
;; внутренняя структура каждого ответа: это просто map c ключами: :time :answer :question :questionkit-name :answer-today-name


(defn- question-title-editor [title-question]
  (ef/html
           [:div.row
             [:div.col-md-3 ]
             [:div.col-md-6
               [:input.form-control.input-md.answer-0 {:autofocus "autofocus" :id "title-question" :name "title-question" :type "text" :placeholder title-question}]
               [:span.help-block "заголовок всей серии ответов"]]
             [:div.col-md-3]] ))


;; https://github.com/writepavel/bootstrap3-wysihtml5-bower#buttons

;; $('#some-textarea').wysihtml5({
;;     "font-styles": true, //Font styling, e.g. h1, h2, etc. Default true
;;     "emphasis": true, //Italics, bold, etc. Default true
;;     "lists": true, //(Un)ordered lists, e.g. Bullets, Numbers. Default true
;;     "html": false, //Button which allows you to edit the generated HTML. Default false
;;     "link": true, //Button to insert a link. Default true
;;     "image": true, //Button to insert an image. Default true,
;;     "color": false, //Button to change color of font
;;     "blockquote": true, //Blockquote
;;   "size": <buttonsize> //default: none, other options are xs, sm, lg
;; });

;;<textarea class="form-control" id="textarea" name="textarea">default text</textarea>

;; http://thecomputersarewinning.com/post/A-Minimal-ClojureScript-Project/


(defn- html-editor [question-idx]
  (let [name (str "answer-" question-idx)]
    (ef/html
     [:div.row
      [:div.col-md-3 ]
      [:div.col-md-6
       [:textarea {:autofocus "autofocus" :class (str "answer-" question-idx " form-control wysihtml5") :id name :name name :rows 12}]]
      [:div.col-md-3]
     [:script {:type "text/javascript"} (str " $('#" name "').wysihtml5({locale: \"ru-RU\"});") ]
        ;;[:script {:type "text/javascript"} (str " $('#" name "').wysihtml5({locale: \"ru-RU\", toolbar: { fa: " true "}});") ]
      ])
    ;;(.on (.find (.contents ($ ".wysihtml5-sandbox")) "body")
    ;;   "keydown"
    ;;   #(log "__  FROM_WYSIHTML5_EDITOR __ KEYPRESSED " (.-keyCode %)))

  ;;(log "(.find (.contents ($ '.wysihtml5-sandbox')) 'body') is")
   ;; (.dir js/console (.contents ($ '.wysihtml5-sandbox')))
   ;;(.dir js/console (.find (.contents ($ '.wysihtml5-sandbox')) 'body'))
  ;;(.wysihtml5 ($ ".wysihtml5" ) (js-obj "locale" "ru-RU" "toolbar" (js-obj "fa" true)))
  ))

;; Сеанс ответов - это "снимок" текущего состояния и экрана в процессе ответов, чтобы в любой момент можно было вернуться к ответам.
;; Данные сеанса ответов состоят из следующих частей: questionkit, серия ответов, текущих слайд
;; Серия ответов это map где ключ - номер вопроса а значение - map с ответом
;; Один ответ - это map с ключами:  :time :answer :question :questionkit-name :kit-answers-title

;; автозаполнение этого атома!!!
;; https://github.com/ckirkendall/enfocus/blob/master/test/cljs/enfocus/bind_test.cljs#L44 -
(def gameplay-data (atom {}))

(defn- else-one-last-question? []
  (>= (- (count (:questionkit @gameplay-data)) 1) (count (vals (:answers @gameplay-data)))))

(defn- all-question-filled? []
  (let [all-answers (vals @gameplay-data)
        answer-vec (mapv #(:answer %) all-answers)]
   ;; (log "CHECK IF FILLED ANSWERS. all-answers are " (pr-str all-answers) ", result is " (reduce #(and %1 (not= 0 (count %2))) true answer-vec))
      (reduce #(and %1 (not= 0 (count %2))) true answer-vec)))


;; TODO Catch it! Maybe by hiccup-css
(defn- render-dots []
  (ef/at "#question-carousel ol.carousel-indicators li" (ef/set-style :border "1px solid #34495e"))
  (ef/at "#question-carousel ol.carousel-indicators li.active" (ef/set-style :background-color "1px solid #34495e")))

;; ;; Listener включается если появился слайд с последним ответом.
;; ;; Если ответов оствлось больше одного то этт listener выключаем
;; (defn- start-textarea-listener []
;;   (events/listen :change #(log "TEXTAREA CHANGED")))

;; (defn- stop-textarea-listener []
;;   (events/remove-listeners :true))

(defn- show-finish-button []
  (log "all questions FILLED! Congratulations!")
  (ef/at [".answer-success"] (ef/remove-attr :hidden)))

(defn- save-all-to-gameplay-atom []
  (let [new-answers (mapv #(.val ($ (str ".answer-" %)))  (keys @gameplay-data))
          map-new-answers (zipmap (range (count new-answers)) new-answers)]
    ;;(mapv #(swap! gameplay-data assoc-in [% :answer] (get map-new-answers %)) (keys map-new-answers))
    (mapv #(swap! gameplay-data assoc-in [% :answer] (do
                                                      ;; (log "INNER_OF_SWAP! NEW_ANSWER IS " (get map-new-answers %))
                                                       (get map-new-answers %)
                                                       )) (keys map-new-answers))
   ;; (log "save-all-to-gameplay! new gameplay-data is " (pr-str gameplay-data) " === NEW ANSWERS ARE === " map-new-answers)
    ))


(defn- slide-changed []
  (log "_______LISTENER SLIDE CHANGED!!!____________")
  (save-all-to-gameplay-atom)
  (render-dots)
  ;;(if else-one-last-question? (start-textarea-listener) (stop-textarea-listener))
  (when (all-question-filled?) (show-finish-button))
  )

(em/defaction show-next-question []
  (log "_______show-next-question________")
  (.carousel ($ "#question-carousel") "next")
  )

(em/defaction show-previous-question []
  (log "_______show-previous-question____________")
  (.carousel ($ "#question-carousel") "prev")
  )

;; проверяет количество элементов в атоме ответов. Если оно совпадает с количеств
;;ом вопросов то показывает кнопку ГОТОВО
;; Если не все ответы заполнены то показывает индикатор прогресса
;; https://github.com/ckirkendall/enfocus/blob/master/src/cljs/enfocus/bind.cljs#L123
;; https://github.com/ckirkendall/enfocus/blob/master/test/cljs/enfocus/bind_test.cljs#L118
;; (binds/bind-input ...)
;; (em/defaction answer-changed []
;;   (let [is-answer-empty? :true]
;;     (when-not is-answer-empty? (show-finish-button))))

;; Это одна из ключевых функций. Она или рисует кнопку ГОТОВО или рисует слайдер прогресса (в первой версии можно без слайдера)
;; Алгоритм выявления, нужно ли рисовать кнопку ГОТОВО
;; 1) Когда входим в режим ответов, включается listener смены состояния класса ACTIVE на слайдах
;; 2) при смене слайда отрисовывается цвет точки
;; 3) если перешли к слайду с последним вопросом (не по номеру а по количеству заполненных ответов!) то
;; 3-1) включаем динамическую игру (то есть заускаем listener): если последний ответ пустой, то слайдер, если не пустой то кнопка ГОТОВО
;; 3-2) Можно написать "остался один ответ! вы на финишной прямой!"

;; http://vvz.nw.ru/Lessons/JavaScript/events.htm - хелп по событиям. Список
;; можно или поймать как обрабатывается и где обраабатывается нажатие на стрелочки - и рядом с нажатием на стрелочки - просто добавить новый обработчик того же самого события.

(defn- progress-indicator []
  "HERE IS PROGRESS_SLIDER")

;; Сеанс ответов - это "снимок" текущего состояния и экрана в процессе ответов, чтобы в любой момент можно было вернуться к ответам.
;; Данные сеанса ответов состоят из следующих частей: questionkit, серия ответов, текущих слайд
;; Серия ответов это map где ключ - номер вопроса а значение - map с ответом
;; Один ответ - это map с ключами:  :time :answer :question :questionkit-name (kit-answer-title то первый ответ)

;; (at "input[name='a']" (bind-input atm {:event :change}))
;; (at "textarea" (bind-input atm {:event :change}))



;; ;; ВО ВРЕМЯ БИНДИНГА - КОГДА КАЖДЫЙ ОТВКЕТ _ ТОГДА МЕРЖ,
;; (defn- bind-question-with-gameplay-data [num question qk-name]
;;   (let [cur-answer (atom "")] ;;; TODO CHECK
;;         ()
;;         (swap! gameplay-data merge new-answermap)
;;         (log "_INIT_QUESTION_IN_GAMEPLAY_DATA gameplay-data is " gameplay-data)))

(em/defsnippet answer-item "template-index.html" "#itemslide2" [question num length qkname answer-atom]
["div.item"]
  (ef/do->
   (ef/remove-attr :style)
   (when (not= num 0) (ef/remove-class "active")))
  ["h2.animated-item-1"] (ef/content question)
  ["p.animated-item-2"] (ef/content "")
  ["div.carousel-content"]
   (ef/do->
    (ef/append (if (= num 0) (question-title-editor question) (html-editor num)))
 ;;   (ef/append    (ef/html [:p (str "SLIDER! Value of "  num " is " question ", length is " length ", ATOM  IS " (pr-str @answer-atom))]))
   ;; (ef/append (progress-indicator))
     (ef/append (ef/html [:div.answer-success {:hidden "hidden"} [:p "Отлично! Cохраните свои мысли в Evernote. Скоро вы выйдите на основной экран приложения." ]
                                              [:a.btn.btn-default.btn-success.btn-lg
                                                 {:style "color: #fff; border: 1px solid rgba(0, 0, 0, 0.3); border-radius: 4px; " :href "#" :onclick "client.core.try_send_answers()"}
                                                 "Сохранить"
                                               ;;  [:span.glyphicon.glyphicon-log-in]
                                               ]])))
[".answer-success .btn:hover"] (ef/set-style :background-color "#47a447")
[".answer-success .btn"] (ef/set-style :background-color "#5cb85c")
;;    [(str ".answer-" num)] (bind/bind-input answer-atom {:event :change})
)

;; (em/defsnippet slide-indicator "template-intex.html" "#slide-indicator"  [idx]
;;   ["li"] (ef/do->
;;              (ef/set-attr :data-slide-to idx)
;;              (when (= 0 idx) (ef/add-class "active"))))



(defn- slide-indicator [idx]

  (let [paramap {:data-target "#question-carousel" :data-slide-to idx}
        attrs (if (= 0 idx) (merge paramap {:class "active"}) paramap)]
    (ef/html [:li attrs])))

(defn- questionstr [question num length]
  (ef/html [:p (str "SLIDER! Value of "  num " is " question ", length is " length)]))


(em/defsnippet answer-slider2 "template-index.html" "#main-slider" [name title-question questionmap map-answer-atoms]
   ["div.embed-container"] (ef/content "")
   ["div.carousel"] (ef/do->
                     (ef/set-attr :id "question-carousel")
                     (ef/remove-class "wet-asphalt")
                     )

 ;; ["div.carousel-content"] (ef/content "")
  ["ol.carousel-indicators"] (ef/content (map slide-indicator (keys questionmap)))
  ["a.prev, a.next"] (ef/do->
                   (ef/add-class "question-nav")
                   (ef/remove-class "hidden-xs")
                   (ef/set-attr :href "#question-carousel"))
  [".carousel li"] (ef/set-style :border "1px solid #34495e")
  [".carousel li.active"]  (ef/set-style :background-color "#34495e")
  ["div.carousel-inner"] (ef/do->
                          (ef/content
                            (map
                           #(answer-item (last(find questionmap %)) % (count questionmap) name (get map-answer-atoms %))
                          (keys questionmap)                          ))
    ;;                                                 (ef/content (answer-item))
                          )
  [".carousel .wysihtml5-toolbar .btn"] (ef/set-style
                                   :border-top-style "solid"
                                   :border-top-width "1px"
                                   :border-radius "4px"
                                   :color "#737373"
                                   :border "1px solid rgba(0, 0, 0, 0.3)" )
  ;;["div.carousel"]
                          )


;; (em/defsnippet answer-slider-test "template-intex.html" "#main-slider" []
;;      ["div.embed-container"] (ef/content "")
;; ;;  ["div.carousel-content"] (ef/content "")
;;  ;; [".carousel-inner"] (ef/content "SLIDER")
;;   )

;; (em/defaction setup-slide-listener []
;;   ["#question-carousel"] (events/listen "slid.bs.carousel" #(slide-changed))
;;   )

(defn- setup-slide-listener []
  (log "_setup_slide_listener")
  (.on ($ "#question-carousel") "slid.bs.carousel" #(slide-changed))
  ;;
  ;; $('.wysihtml5-sandbox').contents().find('body').on("keydown",function() {
  ;;      console.log("Handler for .keypress() called.");
  ;;  });

)

(em/defaction answer-changed [ev]
  (log "INVOKED EVENT ANSWER CHANGED! EVENT IS " ev))

;; TODO Does not work for textarea...
(em/defaction setup-answer-change-listener []
  [".answer-0"] (events/listen :change #(answer-changed %))
  [".answer-1"] (events/listen :change #(answer-changed %))
[".answer-2"] (events/listen :change #(answer-changed %))
  ["#question-carousel"] (events/listen :keydown
                    #(log "__  events/listen-live __ KEYPRESSED " (.-keyCode %))))


(defn create-vecmaps [number qkname questionmap]
  {:number number
    :time (pr-str (js/Date.))
    :qkname qkname
    :question (get questionmap number)
    :answer ""})

(defn- init-gameplay-metainfo [questionmap qkname]
  (let [vec-maps (map #(create-vecmaps % qkname questionmap) (keys questionmap))
        prepared-gameplay-data (zipmap (keys questionmap) vec-maps)]
    (log "init-gameplay-data PREPARED IS " (pr-str prepared-gameplay-data))
    (reset! gameplay-data prepared-gameplay-data)))

;; example of such binding took here https://github.com/ckirkendall/enfocus/blob/master/src/cljs/enfocus/bind.cljs#L87

;; k - key - unique description of watcher
;; r - reference - our atom
;; o - old value
;; n - new value

;; TODO DOES NOT INVOKE ADD-WATCH...
(defn- bind-mapansweratoms-with-gameplaydata [map-answer-atoms]
  (log "bind-mapansweratoms-with-gameplaydata! map-answer-atoms  =  " map-answer-atoms " content of first atom is " @(first (vals map-answer-atoms)))
  (map #(add-watch (get map-answer-atoms %) (str "autosave of answer № " %)

                   (fn [k r o n]
                     (let [current-gameplay-map (get @gameplay-data %)
                                new-gameplay-map (merge current-gameplay-map {:answer n})]
                       (log k "old value was " o ", new value is " n)
                      ;;                       (when (not-equal? o n)
                         (do
                           (swap! merge gameplay-data {% new-gameplay-map})
                           (log ".... add-watch continue .... new gameplay-data is " (pr-str @gameplay-data)))
                        ;;                         )
                       )))
       (keys map-answer-atoms)))

(defn- ^:export show_answer_gameplay [name title-question questions-array]
  (let [questions (js->clj questions-array)
        all-questions (into [title-question] questions)
        countrange (range (count all-questions))
        atomvec (map #(atom "") all-questions) ;; will be useful when autobinding textatea's content with atom from this vector
        questionmap (zipmap countrange all-questions)
        map-answer-atoms (zipmap countrange atomvec)]
  ;;(log "HELLO! Length of questions is " (.length questions-array))
;;    (reset! (first atomvec) "INIT_TITLE")
  ;;  (reset! (last atomvec) "INIT_LAST_ANSWER")
;(log "show_answer_gameplay HELLO!  name is " name ", type of questions is " (type questions))

    (ef/at ".container1"
           (ef/do-> (ef/content (site-header-questions))
                    (ef/append (site-page-title name ""))
                    (ef/append (answer-slider2 name title-question questionmap map-answer-atoms))))
    (setup-slide-listener)
    (setup-answer-change-listener)
    (log "before init-gameplay-metainfo vecmaps = " (map #(create-vecmaps % name questionmap) (keys questionmap)))
    (init-gameplay-metainfo questionmap name)
;;(bind-mapansweratoms-with-gameplaydata map-answer-atoms)
    (.carousel ($ "#question-carousel") "pause")

;; TODO DOES NOT CATCH KEY EVENTS
    (events/listen-live :keydown "#question-carousel"
                    #(log "__  events/listen-live __ KEYPRESSED " (.-keyCode %)))
;; (ef/at ".breadcrumb" (ef/content (ef/html
;;                                              [:div ;;{:align "center"}
;;                                              [:a
;;                                                {:href "#" :onclick "client.core.show_questionkits()" }
;;                                                "Выйти к комплекту вопросов    "
;;                                               ;; [:span.glyphicon.glyphicon-gift]
;;                                               ]])))
))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;----------------------------------------------------------
;; Evernote login state
;;----------------------------------------------------


;; TODO rename to evernote-user and move to local-storage. When move to LS encrypt data and use SSL-certificate for safe transfer user access data.

(def evernote-login-status (atom false))

(def evernote-user (atom nil))



(defn- set-evernote-login-status [status]
  (log "set-evernote-login-status RECEIVED EVERNOTE LOGIN STATUS! = " status)
  (reset! evernote-login-status status))

(defn- try-receive-evernote-login-status []
  (GET "/login/evernote-status"  {:handler set-evernote-login-status :error-handler error-handler}))

(defn- set-evernote-user [user]
  (log "set-evernote-user RECEIVED EVERNOTE USER! = " user)
  (reset! evernote-user user))

(defn- try-receive-evernote-user []
  (GET "/login/evernote-user"  {:handler set-evernote-user :error-handler error-handler}))

(defn init-evernote-user []
  (log "init_evernote_user. Storage-available = " storage-available ", type of \"evernote-user\" is " (type (.get storage "evernote-user")) ", HAS_USER = " (not= (or "null" "nil" nil '(nil)) (.get storage "evernote-user")) ", NEW_HAS_USER = " (storage-has-element? "evernote-user"))
  (if (and storage-available (storage-has-element? "evernote-user"))
    (do
      (set-evernote-user (.get storage "evernote-user"))
      (log "init-evernote-user FROM_LOCAL_STORAGE USER IS " (.get storage "evernote-user")))
    (try-receive-evernote-user))
  ;;(init-evernote-login-status)
  )


(defn init-evernote-login-status []
  (if (and storage-available (storage-has-element? "evernote-login-status"))
    (do
      (set-evernote-login-status (.get storage "evernote-login-status"))
      (log "init-evernote-login-status FROM_LOCAL_STORAGE LOGIN_STATUS IS " (.get storage "evernote-login-status")))
    (try-receive-evernote-login-status)))


(defn is-logged-in []
  (log "invoked is-logged-in. evernote user is  " @evernote-user)
  (not-empty @evernote-user)
  ;;@evernote-login-status
  )


;;----------------------------------



(defn ^:export render-page []

  (if (is-logged-in)
    (do
      (show-questionkits)
      (render-questionkit-list @questionkit-list)
      (try-load-questionkits))
    (show-index))
    (ef/at ".container1"
           (ef/do-> (ef/append "
<!-- Yandex.Metrika counter -->
<script type=\"text/javascript\">
(function (d, w, c) {
    (w[c] = w[c] || []).push(function() {
        try {
            w.yaCounter25841003 = new Ya.Metrika({id:25841003,
                    webvisor:true,
                    clickmap:true,
                    trackLinks:true,
                    accurateTrackBounce:true,
                    ut:\"noindex\"});
        } catch(e) { }
    });

    var n = d.getElementsByTagName(\"script\")[0],
        s = d.createElement(\"script\"),
        f = function () { n.parentNode.insertBefore(s, n); };
    s.type = \"text/javascript\";
    s.async = true;
    s.src = (d.location.protocol == \"https:\" ? \"https:\" : \"http:\") + \"//mc.yandex.ru/metrika/watch.js\";

    if (w.opera == \"[object Opera]\") {
        d.addEventListener(\"DOMContentLoaded\", f, false);
    } else { f(); }
})(document, window, \"yandex_metrika_callbacks\");
</script>
<noscript><div><img src=\"//mc.yandex.ru/watch/25841003?ut=noindex\" style=\"position:absolute; left:-9999px;\" alt=\"\" /></div></noscript>
<!-- /Yandex.Metrika counter -->
")
(ef/append " <script>
  (function(i,s,o,g,r,a,m){i[\"GoogleAnalyticsObject\"]=r;i[r]=i[r]||function(){
  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
  })(window,document,\"script\",\"//www.google-analytics.com/analytics.js\",\"ga\");

  ga(\"create\", \"UA-51159702-3\", \"auto\");
  ga(\"send\", \"pageview\");
</script>
"
)))
  ;;(setup-listen)
  )



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



;;-------------finish----------------------------

(defn- success-send-evernote-login-status [status]
  (log "success-send-evernote-login-status SENT EVERNOTE LOGIN STATUS! = " status)
  ;;(reset! evernote-login-status status)
  )

(defn- try-send-evernote-login-status [status]
  (POST "/login/evernote-status"
        {:params {:login-status status}
          :handler success-send-evernote-login-status
          :error-handler error-handler}))


(defn- success-send-evernote-user [status]
  (log "success-send-evernote-user SENT EVERNOTE LOGIN USER! Responce about login status from server: " status)
  ;;(reset! evernote-login-status status)
  )

(defn- try-send-evernote-user [user]
  (POST "/login/evernote-user"
        {:params {:en-user user}
          :handler success-send-evernote-user
          :error-handler error-handler}))

;; k - key - unique description of watcher
;; r - reference - our atom
;; o - old value
;; n - new value
(add-watch evernote-login-status "evernote-login-status autosave"
           (fn [k r o n]
             (log k "old value was " o ", new value is " n)
             (when (not-equal? o n)
               (do
                 (when storage-available (set-evernote-login-status n))
                 (try-send-evernote-login-status n)
                 (client.core/render-page)))))

(add-watch evernote-user "evernote-user autosave"
           (fn [k r o n]
             (log k "old value was " o ", new value is " n)
             (when (not-equal? o n)
               (do
                 (log k "  __ evernote user changed! __")
                 (when storage-available (set-evernote-user n))
                 (try-send-evernote-user n)
                 (client.core/render-page)))))

(add-watch questionkit-list "questionkit-list show-up-to-date"
           (fn [k r o n]
             (log k "old value was" o ", new value is " n ". not-equal? " (not-equal? o n))
             (when (not-equal? o n)
               (do
                 (render-questionkit-list n)
                 (try-post-questionkit-list n)))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



(defn ^:export evernote_gift []
  (.open js/window "http://goo.gl/NZsMwD"))

(defn ^:export login_by_evernote []
  (set! (.-pathname js/window.location) "/login/evernote"))


(defn ^:export try-create-questionkit [])

(defn ^:export logout []
  (set-evernote-login-status false)
  (set-evernote-user nil))


(defn- success-send-answers [status]
  (log "success-send-answers. Reloading page")
  (logout)
;;  (.reload (.-location js/window))
  ;;(reset! evernote-login-status status)
  )

(defn- try-send-answers []
    (POST "/answers"
        {:params {:answers @gameplay-data}
          :handler success-send-answers
          :error-handler error-handler}))


 (defn start []
   (init-evernote-user)
;;   (init-app-state)
   (render-page))


;; (set! (.-onload js/window) start)
(set! (.-onload js/window) #(em/wait-for-load (start)))
