(ns client.dicts
  (:require [tongue.core :as tongue]))

(def dicts
  { :en { :template-page-name "about-us.html"
          :enter-button "Enter with my Evernote"
         :register-button "I haven't Evernote yet"
         :enter "Enter     "
         :registration "Registration  "
         :logout "Logout"
         :still-empty-qk "You haven't question kit yet. Fill the form to the right to create this one."
         :add-new "Add new"
         :choose-kit "Choose Question Kit"
         :editor "Editor"
         :prepared-with-qk-tag "Prepared with notes wuth \"questionkit\" tag"
         :buttons-slow-load "Buttons can be loading for a minute"
         :question-kits "Question Kits"
         :title-of-answer-series "Title of answer series"
         :to-main-page-soon "Great! You will be back to the main application page soon."
         :save "Save"


         }
   :ru  { :template-page-name "about_us_ru.html"
          :enter-button "Войти через свой Evernote"
         :register-button "У меня ещё нет Evernote"
         :enter "Войти     "
         :registration "Регистрация    "
         :logout "Выйти"
         :still-empty-qk "У вас ещё нет комплекта вопросов. Создайте его, заполнив форму справа."
         :add-new "Добавить новый"
         :choose-kit "Выберите комплект"
         :editor "Редактор"
         :prepared-with-qk-tag "подготовлено из заметок с тэгом  \"questionkit\""
         :buttons-slow-load "Кнопки могут загружаться из Evernote в течение минуты"
         :question-kits "Комплекты вопросов"
         :title-of-answer-series "заголовок всей серии ответов"
         :to-main-page-soon "Отлично! Cохраните свои мысли в Evernote. Скоро вы выйдите на основной экран приложения."
         :save "Сохранить"


         }
   })

(def translate ;; [locale key & args] => string
  (tongue/build-translate dicts))
