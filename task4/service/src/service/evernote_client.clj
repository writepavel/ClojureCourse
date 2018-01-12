(ns service.evernote_client
  (:use
   clojurenote.notes
   clojurenote.enml
   carica.core
   hiccup.core
   service.helpers
   pl.danieljanus.tagsoup
   ))


;; Map of user data: sessionid - en-data.
(def en-sessions (atom {
                        "sessionid" {
                                     :en-user nil
                                     :notebook-guid nil
                                     :all-notes-bean nil
                                     :all-tags nil}}))

(defn set-data [sessionid key value]
  ;;(println (str "\n\n\nBEFORE SET-DATA. \n\n-----------\n\n          sessionid = " sessionid ", key = " key ", value = " value))
  (swap! en-sessions assoc-in [sessionid key] value)
;;  (println (str "\n\n\nAFTER_SET_DATA. en-sessions = " @en-sessions))
  )

(defn get-data [sessionid key]
  (get-in @en-sessions [sessionid key]))


;; Save this info in atom with session_id or in session on client side. When there is no adequate securing with ssl, this will be saved on server side.
(def en-user (atom nil))

;; Save this info in atom with session_id or in session on client side. When there is no adequate securing with ssl, this will be saved on server side.
(def notebook-guid (atom nil))

;; все заметки нашего блокнота. Пригодится для поиска заметок с нужными тэгами и заголовком.
;; Save this info in atom with session_id or in session on client side. When there is no adequate securing with ssl, this will be saved on server side.
(def all-notes-bean (atom nil))

;; Save this info in atom with session_id or in session on client side. When there is no adequate securing with ssl, this will be saved on server side.
(def all-tags (atom nil))

(defn __init-notebook-guid []
  (let
      [notebook (find-notebook-by-name @en-user (config :notebook-name))]
    (if notebook
      (reset! notebook-guid (.getGuid notebook))
      (reset! notebook-guid (.getGuid (create-notebook @en-user (config :notebook-name)))))
      (reset! all-notes-bean
            (map (comp bean (partial get-note @en-user) :guid bean) (basic-notes-for-notebook @en-user @notebook-guid)))
            (reset! all-tags (into []  (get-all-tags-for-notebook @en-user @notebook-guid)))
            ))


(defn init-notebook-guid [sessionid]
  (let
      [en-user (get-data sessionid :en-user)
       notebook (find-notebook-by-name en-user (config :notebook-name))]
    (if notebook
      (set-data sessionid :notebook-guid (.getGuid notebook))
      (set-data sessionid :notebook-guid (.getGuid (create-notebook en-user (config :notebook-name)))))
      (set-data sessionid :all-notes-bean
            (map (comp bean (partial get-note en-user) :guid bean) (basic-notes-for-notebook en-user (get-data sessionid :notebook-guid))))
            (set-data sessionid :all-tags (into []  (get-all-tags-for-notebook en-user (get-data sessionid :notebook-guid))))
            ))


(defn evernote-config []
  {
    :key (get env "thinkhabit_key")
    :secret (env "thinkhabit_secret")
    :callback (str "http://" (get env "thinkhabit_host") "/evernote-oauth-callback")
    ; Delete this, or set to false, to run on production Evernote server
   :use-sandbox false
   })

(defn- note-contains-tags? [note tag-guid-vector]
  (let [note-tags (into #{} (:tagGuids note))]
    (reduce #(and %1 (contains? note-tags %2)) true tag-guid-vector)))

;; {:tagGuids ["d70e7a7b-2988-49bc-97b5-7005c479f0ed" "f507a233-d1ea-416f-9afa-0e73ae23edab"]

(defn- ___filter-notes-by-tags [notes tag-vector]
  (let [tag-name-guid (zipmap
                                              (map #(.getName %) @all-tags)
                                              (map #(.getGuid %) @all-tags))
        tag-guid-vector (map #(get tag-name-guid %) tag-vector)]
  ;;  (println "---invoked filter-notes-by-tag. Tag vector is " tag-vector ", tag-name-guid = " tag-name-guid ", tag-guid-vector = " tag-guid-vector ", FILTER_RESULT IS " (filter #(note-contains-tags? % tag-guid-vector) @all-notes-bean))
    (filter #(note-contains-tags? % tag-guid-vector) @all-notes-bean)))


(defn- filter-notes-by-tags [sessionid notes tag-vector]
  (let [
        all-tags (get-data sessionid :all-tags)
        all-notes-bean (get-data sessionid :all-notes-bean)
        tag-name-guid (zipmap
                                              (map #(.getName %) all-tags)
                                              (map #(.getGuid %) all-tags))
        tag-guid-vector (map #(get tag-name-guid %) tag-vector)]
  ;;  (println "---invoked filter-notes-by-tag. Tag vector is " tag-vector ", tag-name-guid = " tag-name-guid ", tag-guid-vector = " tag-guid-vector ", FILTER_RESULT IS " (filter #(note-contains-tags? % tag-guid-vector) @all-notes-bean))
    (filter #(note-contains-tags? % tag-guid-vector) all-notes-bean)))


(defn- filter-notes-by-title [notes title]
  (filter #(= title (:title %)) notes))


;; https://github.com/mikebroberts/clojurenote
(defn questionkit-list [sessionid]
;;  (println "\n\n\n\nQUESTIONKIT-LIST. Sessionid = " sessionid "\n\nn\n")
   ;;(when @notebook-guid (println "NOTES ARE " (basic-notes-for-notebook @en-user @notebook-guid) ))
  (let [
        notebook-guid (get-data sessionid :notebook-guid)
        all-notes-bean (get-data sessionid :all-notes-bean)]
    (if notebook-guid
        (map #(dissoc % :contentHash :attributes :tagGuidsIterator) (filter-notes-by-tags sessionid all-notes-bean [(config :tag-questionkit)]))
        ;; (map (comp #(dissoc % :contentHash :attributes :tagGuidsIterator) bean (partial get-note @en-user) :guid bean) (basic-notes-for-notebook @en-user @notebook-guid))
        (do (init-notebook-guid sessionid) (questionkit-list sessionid)))))

(defn create-html-note [sessionid notebook-guid title body]
  (let [en-user (get-data sessionid :en-user)]
    (write-note en-user notebook-guid title (create-enml-document body))))

(defn- add-new-note [sessionid title content date-object tag-vector]
  (println "\n\n\n------- \n\n\n  invoked add-new-note. \n\ntitle = " title ", \n\ncontent = " content ", \n\ndate-object = " date-object ", \n\ntag-vector = " tag-vector "\n\n\n")
  (let [
        en-user (get-data sessionid :en-user)
        notebook-guid (get-data sessionid :notebook-guid)]
    (write-note
   en-user
   notebook-guid
   title
   (create-enml-document content)
   (.getTime date-object)
   tag-vector)))

;; @en-user is  {:access-token S=s1:U=8e789:E=14e0bd381e7:C=146b4225398:P=185:A=pashapm-6856:V=2:H=aa828fe0a0368b2ed4f675d3af66d2ae, :notestore-url https://sandbox.evernote.com/shard/s1/notestore}

(defn- update-existing-note [sessionid guid title content date-object]
  (let [
        en-user (get-data sessionid :en-user)
        note (get-note en-user guid)]
    ;;(println "--------   invoked update-existing-note guid = " guid ", title = " title ", content = " content ", date-object = " date-object)
    (if note (do
       ;;        (println "\n\n UPDATE_NOTE INVOKED. (note-store @en-user) = " (note-store @en-user) ", (access-token @en-user) = " (access-token @en-user) ", note = " note )
               ;; https://github.com/evernote/evernote-sdk-java/blob/master/sample/client/EDAMDemo.java#L293
                (.unsetContent note)
                (.unsetResources note)
                (.setTitle note title)
                (.setContent note (create-enml-document content))
                (.setUpdated note (.getTime date-object))
                (.updateNote (note-store en-user)  (access-token en-user)    note)
               ;; (println "\n\n UPDATE_NOTE AFTER = " note ", FROM_SERVER NOTE IS " (get-note @en-user guid))
                )
        (println "\n\n NOTE NOT FOUND!  (get-note en-user guid) = "  (get-note en-user guid) ))))



;; https://github.com/mikebroberts/clojurenote
(defn __questionkit-list []
   ;;(when @notebook-guid (println "NOTES ARE " (basic-notes-for-notebook @en-user @notebook-guid) ))
      (if @notebook-guid
        (map #(dissoc % :contentHash :attributes :tagGuidsIterator) (filter-notes-by-tags @all-notes-bean [(config :tag-questionkit)]))
        ;; (map (comp #(dissoc % :contentHash :attributes :tagGuidsIterator) bean (partial get-note @en-user) :guid bean) (basic-notes-for-notebook @en-user @notebook-guid))
        (do (init-notebook-guid) (questionkit-list))))

(defn __create-html-note [notebook-guid title body]
  (write-note @en-user notebook-guid title (create-enml-document body)))

(defn- __add-new-note [title content date-object tag-vector]
 ;; (println "-------   invoked add-new-note. title = " title ", content = " content ", date-object = " date-object ", tag-vector = " tag-vector)
  (write-note
   @en-user
   @notebook-guid
   title
   (create-enml-document content)
   (.getTime date-object)
   tag-vector))

;; @en-user is  {:access-token S=s1:U=8e789:E=14e0bd381e7:C=146b4225398:P=185:A=pashapm-6856:V=2:H=aa828fe0a0368b2ed4f675d3af66d2ae, :notestore-url https://sandbox.evernote.com/shard/s1/notestore}

(defn- __update-existing-note [guid title content date-object]
  (let [note (get-note @en-user guid)]
    ;;(println "--------   invoked update-existing-note guid = " guid ", title = " title ", content = " content ", date-object = " date-object)
    (if note (do
       ;;        (println "\n\n UPDATE_NOTE INVOKED. (note-store @en-user) = " (note-store @en-user) ", (access-token @en-user) = " (access-token @en-user) ", note = " note )
               ;; https://github.com/evernote/evernote-sdk-java/blob/master/sample/client/EDAMDemo.java#L293
                (.unsetContent note)
                (.unsetResources note)
                (.setTitle note title)
                (.setContent note (create-enml-document content))
                (.setUpdated note (.getTime date-object))
                (.updateNote (note-store @en-user)  (access-token @en-user)    note)
               ;; (println "\n\n UPDATE_NOTE AFTER = " note ", FROM_SERVER NOTE IS " (get-note @en-user guid))
                )
        (println "\n\n NOTE NOT FOUND!  (get-note @en-user guid) = "  (get-note @en-user guid) ))))



(defn- add-new-questionkit [sessionid notemap]
;;(println "add-new-questionkit @en-user is " @en-user)
  (add-new-note sessionid
   (:title notemap)
   (:content notemap)
   (java.util.Date.)
   [(config :tag-questionkit)]))

(defn- questionkit-exists? [notemap]
                     (not (nil? (:guid notemap))))

(defn- update-existing-questionkit [sessionid notemap]
  (update-existing-note sessionid (:guid notemap) (:title notemap) (:content notemap) (java.util.Date.)))

(defn try-update-questionkit [sessionid notemap]
  ;; (println "invoked try-update-questionkit-list. Received from client: " notemap)
   (if (questionkit-exists? notemap)
     (update-existing-questionkit sessionid notemap)
     (add-new-questionkit sessionid notemap)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;  SAVING ANSWERS
;;
;; нажатие на кнопке "готово"
;;    - создается заметка с двумя тэгами "имя_списка_вопросов" и "group_by_time" и заголовком "заголовок_ответа"
;;    - ищется заметка с двумя тегами "имя_списка_вопросов" и "group_by_question" и заголовком "вопрос для ответа" . Если найдена, то добавляется ответ в конец. Если не найдена, то создается новая.
;; внутренняя структура каждого ответа: это просто map c ключами: :time :answer :question :questionkit-name :answer-today-name

;; add-new-note [title content date-object tag-vector]

;; Поиск по сохранённым в атоме заметкам
(defn- get-guid [sessionid tag-vector title]
  (let [
        all-notes-bean (get-data sessionid :all-notes-bean)
        tags-filtered (filter-notes-by-tags sessionid all-notes-bean tag-vector)
         tags-title-filtered (filter-notes-by-title tags-filtered title)
          first-note (first tags-title-filtered)]
;;      (println "invoked GET-GUID of note with title \"" title "\" and tag-vector " tag-vector ". All-tags: " @all-tags ", filtered-notes are " tags-title-filtered)
  (:guid first-note)))

(defn- local-get-note-by-guid [sessionid guid]
  (let [all-notes-bean (get-data sessionid :all-notes-bean)]
   (first (filter #(= guid (:guid %)) all-notes-bean))))

(defn- __get-guid [tag-vector title]
  (let [tags-filtered (filter-notes-by-tags @all-notes-bean tag-vector)
         tags-title-filtered (filter-notes-by-title tags-filtered title)
          first-note (first tags-title-filtered)]
;;      (println "invoked GET-GUID of note with title \"" title "\" and tag-vector " tag-vector ". All-tags: " @all-tags ", filtered-notes are " tags-title-filtered)
  (:guid first-note)))

(defn- __local-get-note-by-guid [guid]
  (first (filter #(= guid (:guid %)) @all-notes-bean)))


;; content of note
;; <?xml version="1.0" encoding="UTF-8"?><!DOCTYPE en-note SYSTEM "http://xml.evernote.com/pub/enml2.dtd"><en-note><h2>??????????</h2>?????<br/></en-note><h2>????????</h2>?????????<br/>

(defn- extract-inner-ennote-html [data]
  (let [hiccup-data (children (parse-string data))]
    ;;(println "------!!!!!  invoked extract-inner-ennote-html hiccup-data is " hiccup-data)
    (children (parse-string data))))

;; TODO extract only contents of tags <en-note> </en-note>
(defn- existing-note-content [sessionid guid]
  (let [old-content (:content (local-get-note-by-guid sessionid guid))
          old-ennote-innerhtml (extract-inner-ennote-html old-content)]
    (if guid  old-ennote-innerhtml)))

(defn- create-content-by-question [sessionid guid answer-title answer]
  (if guid
      (html (existing-note-content sessionid guid) [:div [:h2 answer-title] answer])
      (html  [:div [:h2 answer-title] answer])))

;; add-new-note [title content date-object tag-vector]
(defn- update-or-create-new-note-by-question [sessionid tag-vector title today-answer-title answer date-object]
  (let [guid (get-guid sessionid tag-vector title)
          content (create-content-by-question sessionid guid today-answer-title answer)]
   ;; (println " ---------  invoked update-or-create-new-note-by-question tag-vector = " tag-vector ", title = " title ", today-answer-title = " today-answer-title ", answer = " answer ", date-object = " date-object ", GUID = " guid ", NEW_CONTENT = " content)
    (if guid
      (update-existing-note sessionid guid title content date-object)
      (add-new-note sessionid title content date-object tag-vector)
      ;; (do
      ;;   (init-notebook-guid)
      ;;   (if (get-guid tag-vector title)
      ;;     (update-existing-note guid title content date-object)
      ;;     (add-new-note title content date-object tag-vector)))
      )))

;; Обновляем заметки
;; update-existing-note [guid title content]


(defn- correct-html [bad-html]
  (clojure.string/replace bad-html #"<br>" "<br/>"))

(defn- correct-answer [answer]
  (update-in answer [:answer] correct-html))


(defn- send-answers-by-question [sessionid answer-data]
  (let [ date-object (read-string (:time (last (vals answer-data))))
         answer-title (:answer (first (vals answer-data)))]
;;    (println "----invoked send-answers-by-question. date-object = " date-object ", answer-title = " answer-title ", (vals answer-data) = " (vals answer-data))
    (doseq [item (vals answer-data)]
      (update-or-create-new-note-by-question sessionid
                           [(:qkname item) (config :tag-group-by-question)]
                           (:question item)
                           answer-title
                           (correct-html (:answer item))
                           date-object))))



;; https://antoniogarrote.wordpress.com/2010/10/20/hiccup-rdfa-semantic-markup-for-clojure-web-apps/
;; http://www.rkn.io/2014/03/13/clojure-cookbook-hiccup/
;; http://clojure-doc.org/articles/tutorials/basic_web_development.html

(defn- create-content-by-time [answer-data]
   (html (map (fn [item] [:div [:h2 (:question item)] (correct-html (:answer item))])  (vals answer-data))))

;; Создаём новую заметку, где контент - это собранные ответы, разделённые заголовками на вопросы.
(defn- send-answers-by-time [sessionid answer-data]
  (let [answerlist {}
        title (:answer (get answer-data 0))
        full-content (create-content-by-time answer-data)
        date-object (read-string (:time (last (vals answer-data))))
        qk-name (:qkname (get answer-data 0))
        tag-vector [qk-name (config :tag-group-by-time)]]
    (add-new-note sessionid title full-content date-object tag-vector)))



;; (def answer-data {0 {:number 0, :time #inst "2014-08-12T19:06:15.303-00:00", :qkname  "???????? ????????", :question "????????? ????????", :answer "q"}, 1 {:number 1, :time #inst "2014-08-12T19:06:15.306-00:00", :qkname "???????? ????????", :question "??????1", :answer "w<br>er"}, 2 {:number 2, :time #inst "2014-08-12T19:06:15.307-00:00", :qkname "???????? ????????", :question "??????2", :answer "as<br>2"}, 3 {:number 3, :time #inst "2014-08-12T19:06:15.307-00:00", :qkname "???????? ????????", :question "??????3", :answer "f"}})





(defn try-send-answers [sessionid answer-data]
 ;; (println "TRY_SEND_ANSWERS. answer-data is " answer-data)

  (send-answers-by-time sessionid answer-data)
  (send-answers-by-question sessionid answer-data)
;;  (println "==========\n\n\n\ninvoked TRY-SEND-ANSWERS AND RUN BY_TIME AND BY_QUESTION! ANSWER_DATA IS " answer-data "\n\n\n\n")
  )
