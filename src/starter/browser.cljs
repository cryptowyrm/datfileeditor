(ns starter.browser
  (:require
    [reagent.core :as r]

    ;; CodeMirror
    ["react-codemirror2" :as code-mirror]
    ["codemirror/mode/javascript/javascript"]
    ["codemirror/mode/markdown/markdown"]

    ;; Material UI styles
    ["material-ui/styles/colors" :as jscolors]
    ["material-ui/styles/getMuiTheme" :as get-theme]
    ["material-ui/styles/MuiThemeProvider" :as theme-provider]

    ;; Material UI icons
    ["material-ui/svg-icons/file/folder" :as icon-folder]
    ["material-ui/svg-icons/file/folder-open" :as icon-folder-open]
    ["material-ui/svg-icons/editor/insert-drive-file" :as icon-file]

    ;; Material UI components
    ["material-ui/List/List" :as list]
    ["material-ui/List/ListItem" :as list-item]
    ["material-ui/AppBar" :as appbar]
    ["material-ui/Paper" :as paper]
    ["material-ui/RaisedButton" :as button]
    ["material-ui/Toggle" :as toggle]
    ["material-ui/Toolbar/Toolbar" :as toolbar]
    ["material-ui/Toolbar/ToolbarGroup" :as toolbar-group]
    ["material-ui/Toolbar/ToolbarSeparator" :as toolbar-separator]
    ["material-ui/Toolbar/ToolbarTitle" :as toolbar-title]))

;; App state

(defonce app-state (r/atom {:files []
                            :archive nil
                            :selected-file nil
                            :wrap-lines false}))

;; Beaker API methods

(defn archive-changed
  "Takes a Dat archive and a callback f which is called with a single boolean
  parameter that is true if the archive has changed files, or false."
  [archive f]
  (-> (.diff archive)
      (.then (fn [result]
               (f (not (empty? (js->clj result))))))))

(defn readdir
  "Takes a dat archive and a path to a directory and calls f with
  a vector of the files in that directory."
  [archive path f]
  (-> (.readdir archive path #js{:stat true})
    (.then #(f (js->clj %)))))

(defn select-archive [f & {:keys [recursive?]}]
  (-> (js/DatArchive.selectArchive)
    (.then
     (fn [archive]
       (js/console.log archive)
       (swap! app-state assoc :archive archive
                              :selected-file nil
                              :selected-file-content nil
                              :selected-file-edited nil)
       (archive-changed archive
        (fn [changed]
          (swap! app-state assoc :changed changed)))
       (.getInfo archive))
     (fn [e]
       (js/console.log "select-archive error: " e)))
    (.then
     (fn [info]
       (swap! app-state assoc :owner (aget info "isOwner"))
       (js/console.log (:owner @app-state))
       (.readdir (:archive @app-state) "/" #js{:stat true
                                               :recursive (boolean recursive?)}))
     (fn [e]
       (js/console.log "select-archive error: " e)))
    (.then
     #(f (js->clj %))
     (fn [e]
       (js/console.log "select-archive error: " e)))))

;; Utility methods

(defn file-tree [files-list]
  (let [directories (filter #(.isDirectory (% "stat")) files-list)
        files (filter #(not (.isDirectory (% "stat"))) files-list)]
    (concat
      (map
        (fn [directory]
          (assoc directory
                 "contents"
                 (filter
                  (fn [file]
                    (.startsWith (file "name") (str (directory "name") "/")))
                  files)))
        directories)
      (filter #(not (clojure.string/includes? (% "name") "/")) files))))

(def colors
  (js->clj jscolors :keywordize-keys true))

(defn browse-daturl [daturl]
  (let [archive (js/DatArchive. daturl)]
    (readdir archive "/"
      (fn [files]
        (js/console.log (clj->js files))
        (swap! app-state assoc :archive archive)
        (swap! app-state assoc :files files)))))

(defn format-bytes [bytes]
  (cond (< bytes 1024) (str bytes " bytes")
        (>= bytes 1024) (str (.toFixed (/ bytes 1024) 2) " kb")
        (>= bytes (* 1024 1024)) (str (.toFixed (/ bytes (* 1024 1024)) 2) " mb")))

;; React Components

(def theme (get-theme/default #js {}))

(defn list-item-file [file]
  (let [expanded (r/atom false)]
    (fn [file]
      [:> list-item/default
        {:primary-text (file "name")
         :left-icon
          (if (.isDirectory (file "stat"))
            (if @expanded
              (r/as-element [:> icon-folder-open/default])
              (r/as-element [:> icon-folder/default]))
            (r/as-element [:> icon-file/default]))
         :secondary-text
          (if (.isDirectory (file "stat"))
            (str (count (file "contents")) " files")
            (format-bytes (aget (file "stat") "size")))
         :nested-items (if (empty? (file "contents"))
                         #js[]
                         (clj->js
                           (for [file (file "contents")]
                             (r/as-element
                               [list-item-file file]))))
         :nested-list-style {:margin-left 10}
         :primary-toggles-nested-list (when-not (empty? (file "contents")) true)
         :on-nested-list-toggle (fn [list]
                                  (swap! expanded not))
         :on-click
          (fn [e]
            (when-not (.isDirectory (file "stat"))
              (swap! app-state assoc :selected-file file)
              (swap! app-state assoc :selected-file-edited nil)
              (-> (.readFile (:archive @app-state) (file "name"))
                  (.then #(swap! app-state assoc :selected-file-content %)))))}])))

(defn files-list []
  (let [archive (r/cursor app-state [:archive])
        files (r/cursor app-state [:files])]
    (fn []
      ^{:key (if @archive
               (.-url @archive)
               (random-uuid))}
      [:> paper/default
        {:z-depth 1
         :style {:padding 0
                 :margin 20
                 :flex 1
                 :overflow-y "auto"}}
        [:> paper/default
          {:z-depth 1}
          [:> list/default
            (for [file (file-tree @files)]
              ^{:key (file "name")}
              [list-item-file file])]]])))

(defn editor []
  (let [selected-file (r/cursor app-state [:selected-file])
        selected-file-content (r/cursor app-state [:selected-file-content])
        selected-file-edited (r/cursor app-state [:selected-file-edited])
        wrap-lines (r/cursor app-state [:wrap-lines])]
    (fn []
      [:div {:style {:flex 1
                     :display "flex"
                     :flex-direction "column"
                     :overflow "hidden"}}
       [:> paper/default
         {:style {:display "flex"
                  :flex-direction "column"
                  :flex 1
                  :padding 0
                  :margin 20
                  :margin-left 0}}
        ^{:key (if @selected-file
                 (@selected-file "name")
                 (random-uuid))}
        [:> code-mirror/UnControlled
          {:value @selected-file-content
           :class-name "react-cm-flex"
           :options {:line-numbers true
                     :line-wrapping @wrap-lines
                     :mode "javascript"}
           :on-change (fn [editor data value]
                        (if-not (= value @selected-file-content)
                          (reset! selected-file-edited value)))}]]])))

(defn dat-input []
  (let [text (atom "")
        daturl (r/atom app-state [:daturl])]
    [:div
      [:input {:type "text"
               :placeholder "dat://..."
               :default-value @text
               :on-change (fn [e] (swap! text #(-> e .-target .-value)))}]
      [:button {:on-click #(browse-daturl @text)}

        "Browse daturl"]]))

(defn app-toolbar []
  (let [wrap-lines (r/cursor app-state [:wrap-lines])
        archive (r/cursor app-state [:archive])
        changed (r/cursor app-state [:changed])
        owner (r/cursor app-state [:owner])
        selected-file (r/cursor app-state [:selected-file])
        selected-file-content (r/cursor app-state [:selected-file-content])
        selected-file-edited (r/cursor app-state [:selected-file-edited])]
    (fn []
      [:> paper/default
        {:z-depth 1
         :style {:margin 20
                 :margin-bottom 0
                 :padding 0}}
        [:> toolbar/default
         [:> toolbar-group/default {:first-child true}
          [:> button/default
            {:on-click (fn []
                         (select-archive
                           #(swap! app-state assoc :files %)
                           :recursive? true))
             :background-color (:grey400 colors)
             :style {:min-width 160}}
            "Open Dat archive"]
          (if-not @owner
            [:> toolbar-title/default {:text "read-only"}])]
         [:> toolbar-group/default
          [:> button/default
            {:background-color (:blue500 colors)
             :disabled (or
                         (not @owner)
                         (not @selected-file-edited))
             :on-click (fn []
                         (reset! selected-file-content @selected-file-edited)
                         (reset! selected-file-edited nil)
                         (-> (.writeFile @archive (@selected-file "name") @selected-file-content)
                             (.then #(reset! changed true))))}
            "Save"]
          [:> button/default
            {:background-color (:blue500 colors)
             :on-click (fn []
                         (reset! changed false)
                         (.commit @archive))
             :disabled (or
                        (not @owner)
                        (not @changed))}
            "Publish"]
          [:div
           [:> toggle/default
             {:label "Wrap lines"
              :toggled @wrap-lines
              :on-toggle (fn [event checked]
                           (reset! wrap-lines checked))}]]]]])))

(defn content []
  (let []
    (fn []
      [:div
        [:> theme-provider/default {:theme theme}
          [:div {:style {:display "flex"
                         :flex-direction "column"
                         :position "absolute"
                         :top 0
                         :left 0
                         :right 0
                         :bottom 0}}
            [:> appbar/default {:title "Dat File Editor"}]
            [app-toolbar]
            [:div
              {:class "twopane"
               :style {:display "flex"
                       :flex-direction "row"
                       :flex 1}}
              [files-list]
              [editor]]]]])))

;; App Lifecycle methods

(defn start []
  ;; start is called by init and after code reloading finishes
  ;; this is controlled by the :after-load in the config

  (let [location (if (= js/window.location.hostname "localhost")
                   "dat://cedc20827cfaa728deb86424dd0e53e4e05b040a734e9cf2367f9e5ff0e4676b"
                   (.toString js/window.location))]
    (browse-daturl location))

  (r/render [content]
            (js/document.getElementById "app")))

(defn ^:export init []
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (start))

(defn stop [])
  ;; stop is called before any code is reloaded
  ;; this is controlled by :before-load in the config
