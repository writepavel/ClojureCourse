
Sources of alpha version of http://thinkhabit.ru service

This servise is evernote client and is made for filling answers to regular questions made by the user himself.

It is usseful for summarize any progress to fill answers to the same questrions weekly, monthly etc.

### How does it work

In user's evernote account new notebook "ThinkHabit" appears to hold everything user input inside thinkhabit.ru service.

First there are created question kits that are storted in notes with "questionkit" evernote tag.

Questionkit is basis of new interface for answering that questions.

Answers are saved in user's evernote twice: by questions and by date

 - by questions: updating the note holding all answers to the same question.
   
   - title of these notes is equal to the question.
   - note has two tags: "by_question" and "QUESTIONKIT_NAME"
   
 - by time: creates new note with all "today" answers to particular questionkit.
 
   - title of these notes ai equal to answer to first question "Answer kit title"
   - note has two tags: "by_time" and "QUESTIONKIT_NAME"

### How to use service

1. Make up questionkit: 

  - name. E.g. "weekly review", or "hew book is read"
  - question for today answers title. E.g. "today's date" or "Book name"
  - other questions. E.g. "what good things did I do?" or "When this book us useful"

2. Write down new questionkit in thinkhabit.ru service

3. After inserting questionkit, new button with it's name will appear. Press it to start answer jorney.

4. Write answers and switch between questions by pressing arrow keys in window borders.

5. That's it! Repeat the same questionkit later if you wish. 

### How to run it on your own hosting

Add environment variables to server: `domain_name`, `thinkhabit_key`, `thinkhabit_secret`
Last two vars should be got from https://dev.evernote.com/doc/articles/dev_tokens.php


From server directory: 

```
lein ring server
```

From client directory
to create ``client/resources/public/js/main.js`file run:
```
lein cljsbuild once
```

### TODO

- navigation between questions: by keyboard and by gestures. Now it's just by UI arrow buttons.
- add English language
- add howto and description of service on title page
- performance
- fix "save button" bug

