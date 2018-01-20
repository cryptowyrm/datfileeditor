## Dat File Editor for the Beaker Browser

A little [Dat archive](https://datproject.org/) file editor I'm developing to learn [Beaker Browser](https://beakerbrowser.com/)'s [Dat API](https://beakerbrowser.com/docs/apis/).

A live version of the app can be viewed within the Beaker Browser under the following URL: dat://editor-cryptic.hashbase.io

![Screenshot](https://i.imgur.com/DPnyPFn.png)

Developed with [ClojureScript](https://clojurescript.org/) & [Reagent](https://reagent-project.github.io/) (minimalistic wrapper on top of [React](https://reactjs.org/)). Uses [CodeMirror](https://codemirror.net/) for the editor component, so that code you edit is syntax highlighted.

This project is built on top of the [Shadow-CLJS browser quickstart template](https://github.com/shadow-cljs/quickstart-browser), so it uses [Shadow-CLJS](http://shadow-cljs.org/) as the build tool instead of Leiningen or boot. The reason for choosing Shadow-CLJS is its better support for npm packages, should make working with Beaker libraries like [WebDB](https://github.com/beakerbrowser/webdb) easier. Also, I wanted to learn how to use it since I already know Leiningen and boot.

This Readme is also a modified version of the Readme from the template since it explains very well how to easily use Shadow-CLJS if you've never used it before.

## Required Development Software

- [node.js v6.0.0+](https://nodejs.org/en/download/)
- [Java8](http://www.oracle.com/technetwork/java/javase/downloads/index.html) (Java9 has some issues, so stick with Java SE 8 for now). OpenJDK also works.

## Running the app for development

Clone this repository and then execute the following command in a terminal inside the cloned folder to install the needed dependencies:

```bash
npm install
```
Now execute the following command to open up a Clojure REPL for the project:

```bash
npx shadow-cljs clj-repl
```

To start the development server which automatically compiles code when you change it and hot reloads the changes into the running website, execute the following command:

```txt
(shadow/watch :app)
```

You do not have to do this at the REPL, you can also run `npx shadow-cljs watch app` in another terminal. The result will be the same.

Either way you should see a message like this:

```txt
[:app] Build completed. (23 files, 4 compiled, 0 warnings, 7.41s)
```

When you do, you can open the following URL in the Beaker Browser to see the app: http://localhost:8020

You can now start making changes to the code and the changes will get hot reloaded in Beaker Browser when you save.

## REPL

To switch the running Clojure repl to the ClojureScript REPL to interact with the app:

```
(shadow/repl :app)
```

Or from the command line use `npx shadow-cljs cljs-repl app`.

This can now be used to eval code in the browser (assuming you still have it open). Try `(js/alert "Hi.")` and take it from there.

You can get back to the Clojure REPL by typing `:repl/quit`.

## Release

The `watch` process is all about development. It injects the code required for the REPL and all other devtools but we do not want any of that when putting the code into production.

The `release` action will remove all development code and run the code through Google's Closure Compiler to produce a minified `main.js` file. Since that will overwrite the file created by the `watch` we first need to stop that.

```
(shadow/stop-worker :app)
(shadow/release :app)
```

Or in the command line stop the `npx shadow-cljs watch` process by CTRL+C and then `npx shadow-cljs release app`.

When done you can open `http://localhost:8020` and see the `release` build in action. At this point you can copy the `public` directory to a new site created in the Beaker Browser to publish it.
