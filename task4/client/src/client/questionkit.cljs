(ns client.questionkit
   (:require                 [client.helpers :refer [log]]
                             [dommy.utils :as utils]
  ;;                           [dommy.template :as template]
                             [dommy.core :as dommy]
;;                             [enfocus.core :as ef]
;;                 [enfocus.events :as events]
                                              )
     (:use-macros
    [dommy.macros :only [node sel sel1]])
   (:require-macros [enfocus.macros :as em])
  )

;; Преобразование "комплектов вопросов" в разные форматы.
;; note - для использования evernote. Парсится контент с помощью dommy.
;; Удобнее пользоваться enfocus но пока не научился в enfocus читать не
;; шаблон из внешнего html-файла, а произвольную строку.
;; formdata - для использования "редактора" в виде html-формы

(defn- join-with [joiner str1 str2]
;;  (log "__ join str1 = " str1 ", str2 = " str2)
  (if (= PersistentArrayMap (type str1))
  str2
  (if str1 (.concat str1 joiner str2) nil)))

;; (defn qk->html [qkdata]
;;   (let [elemvec1 (conj [] ;;qkdata
;;                         (:name qkdata)
;;                         (:answer-name qkdata))
;;         elemvec2 (into elemvec1 (:questions qkdata))]
;;    ;; (log "invoked qk->html. Elemvec = " elemvec2 ", got from QK_DATA = " qkdata)
;;     (reduce (partial join-with "<br/>") elemvec2)))

(defn qk->formdata [qkdata]
  {:name (:name qkdata)
    :answer-name (:answer-name qkdata)
    :kit-questions-area (reduce join-with "\n" (:questions qkdata))})


(defn vec_trim_lines_by [splitter textarea]
  (log "invoked vec_trim_lines_by. Splitter = " splitter ", textarea = " textarea)
  (let [lines (.split textarea splitter)]
    (log "vec_trim_lines. lines = " lines ", got from textarea: " textarea)
  (vec (remove empty? lines))))


;; (defn html->qk [htmldata]
;;   (let [htmllines htmldata
;;         vecdata (vec_trim_lines_by "<br/>" htmllines)]
;;     {:name (first vecdata)
;;      :answer-name (second vecdata)
;;      :questions (subvec vecdata 2)}))

(defn formdata->qk [formdata]
  {:name (:name formdata)
   :answer-name (:answer-name formdata)
   :questions (vec_trim_lines_by "\n" (:kit-questions-area formdata))})

;;-----------------------------------------------------------------------
;; TODO migrate from dommy to enfocus
;; see video of Creighton Kirkendall
;; HOW TO parse dom from string in enfocus without using external html-files?

;; (defn a-node []
;;   (ef/html [:br]))

;; (defn my-row [person]
;;   (node [:tr [:td {:class "name"} (:name person)]
;;                   [:td {:class "title"} (:title person)]
;;                   [:td {:class "color"} (:color person)]]))

;; (defn populate-table [tbl data]
;;   (ef/at tbl
;;          "tbody tr" (remove-node)
;;          "tbody" (content (map my-row data))
;;          "tbody tr:nth-of-type(even)" (add-class "even")))
;;--------------------------------------------------------------

;; returns array of HTMLDivElement
;; maybe will be useful "replace-contents!" https://github.com/Prismatic/dommy/blob/master/src/dommy/core.cljs#L130
;; "node-like" - forms node https://github.com/Prismatic/dommy/blob/master/src/dommy/template.cljs#L71

(defn note-div-array [content]
  (let [simple_node (node [:br])]  ;; create blank node
    (dommy/set-html! simple_node content) ;; fill new node by content
    (sel simple_node :div) ;; create vector of div nodes
   ;; (str "__SELECT_NOTE_DIV type of DD is " (type divs ) ",  selection is " divs " Is it array? " (array? divs) )
    ))

(defn vector-lines [htmlarray]
  (let [node-vec (into [] htmlarray)]
    (mapv #(dommy/text %) node-vec)))

(defn note->qk [notebean]
  (log "___ note->qk _____ parsed notebean = " ((comp vector-lines note-div-array) (:content notebean)))
  (let [lines ((comp vector-lines note-div-array) (:content notebean))]
    {:name (:title notebean)
      :answer-name (first lines)
      :questions (rest lines)
      :note-guid (:guid notebean)}))

(defn note-body [qkdata]
  (let [firstline (:answer-name qkdata)
         restlines (:questions qkdata)
       ;;  body (node [:en-note])
        ;; body-first (if firstline (dommy/append! body [:div firstline]) nil)
       ;; body-rest (if restlines (map #(dommy/append! body [:div %]) restlines) nil)
        ]
    ;;(dommy/append! body firstline)
   ;; (when body-first (reduce #(when %2(dommy/insert-after! %1 %2)) body-first body-rest))

    ;;(dommy/html body)

   (when firstline
     (reduce
      #(when %2(str %1 "<div>" %2 "</div>" ))
      (str "<div>" firstline "</div>") restlines))))


(defn qk->note [qkdata]
  {:title (:name qkdata)
    :content (note-body qkdata)
    :guid (:note-guid qkdata)})
