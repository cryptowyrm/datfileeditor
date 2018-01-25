(ns starter.browser
  (:require
    [reagent.core :as r]

    ;; CodeMirror
    ["react-codemirror2" :as code-mirror]
    ["codemirror/mode/javascript/javascript"]
    ["codemirror/mode/markdown/markdown"]
    ["codemirror/mode/coffeescript/coffeescript"]
    ["codemirror/mode/css/css"]
    ["codemirror/mode/xml/xml"]
    ["codemirror/mode/shell/shell"]
    ["codemirror/mode/htmlmixed/htmlmixed"]

    ;; Material UI styles
    ["material-ui/styles/colors" :as jscolors]
    ["material-ui/styles/getMuiTheme" :as get-theme]
    ["material-ui/styles/MuiThemeProvider" :as theme-provider]
    ["material-ui/styles/baseThemes/lightBaseTheme" :as light-base-theme]
    ["material-ui/styles/baseThemes/darkBaseTheme" :as dark-base-theme]

    ;; Material UI icons
    ["material-ui/svg-icons/file/folder" :as icon-folder]
    ["material-ui/svg-icons/file/folder-open" :as icon-folder-open]
    ["material-ui/svg-icons/editor/insert-drive-file" :as icon-file]

    ;; Material UI components
    ["material-ui/List/List" :as list]
    ["material-ui/List/ListItem" :as list-item]
    ["material-ui/AppBar" :as appbar]
    ["material-ui/Drawer" :as drawer]
    ["material-ui/Slider" :as slider]
    ["material-ui/Paper" :as paper]
    ["material-ui/RaisedButton" :as button]
    ["material-ui/Toggle" :as toggle]
    ["material-ui/Toolbar/Toolbar" :as toolbar]
    ["material-ui/Toolbar/ToolbarGroup" :as toolbar-group]
    ["material-ui/Toolbar/ToolbarSeparator" :as toolbar-separator]
    ["material-ui/Toolbar/ToolbarTitle" :as toolbar-title]))

(set! *warn-on-infer* true)

;; App state

(defn default-settings []
  {:font-size 12
   :dark-theme false})

(defonce app-state (r/atom {:files []
                            :archive nil
                            :selected-file nil
                            :wrap-lines false
                            :changed-files {}
                            :drawer-open false
                            :settings (default-settings)}))

;; Filetypes
(def filetypes-image
  ["jpg" "jpeg" "png" "gif"])

(def filetypes-codemirror
  {"md" "markdown"
   "js" "javascript"
   "json" "javascript"
   "coffee" "coffeescript"
   "css" "css"
   "html" "htmlmixed"
   "xml" "xml"
   "sh" "shell"})

;; Beaker API methods

(defn archive-changed
  "Takes a Dat archive and a callback f which is called with a single boolean
  parameter that is true if the archive has changed files, or false."
  [^js archive f]
  (-> (.diff archive)
      (.then (fn [result]
               (f (not (empty? (js->clj result))))))
      (.catch
       (fn [e]
         (js/console.log "archive-changed error: " e)))))

(defn readdir
  "Takes a dat archive and a path to a directory and calls f with
  a vector of the files in that directory."
  [^js archive path f]
  (-> (.readdir archive path #js{:stat true
                                 :recursive true})
    (.then #(f (js->clj %)))
    (.catch
     (fn [e]
       (js/console.log "readdir error: " e)))))

(defn select-archive [f & {:keys [recursive?]}]
  (-> (js/DatArchive.selectArchive)
    (.then
     (fn [^js archive]
       (swap! app-state assoc :archive archive
                              :selected-file nil
                              :selected-file-content nil
                              :selected-file-edited nil
                              :changed-files {})
       (archive-changed archive
        (fn [changed]
          (swap! app-state assoc :changed changed)))
       (.getInfo archive)))
    (.then
     (fn [info]
       (swap! app-state assoc :owner (aget info "isOwner"))
       (.readdir ^js (:archive @app-state) "/" #js{:stat true
                                                   :recursive (boolean recursive?)})))
    (.then
     #(f (js->clj %)))
    (.catch
     (fn [e]
       (js/console.log "select-archive error: " e)))))

;; Utility methods

(defn setting-for [setting-key]
  (let [setting (get-in @app-state [:settings setting-key])]
    (if (nil? setting)
      (do
        (swap! app-state assoc-in [:settings setting-key] (setting-key (default-settings)))
        (setting-key (default-settings)))
      setting)))

(defn load-settings []
  (let [settings (r/cursor app-state [:settings])
        loaded (js/JSON.parse (.getItem js/localStorage "settings"))]
    (if (nil? loaded)
      nil
      (reset! settings (js->clj loaded :keywordize-keys true)))))

(defn save-settings []
  (let [settings (r/cursor app-state [:settings])]
    (.setItem js/localStorage "settings" (js/JSON.stringify (clj->js @settings)))))

(defn file-tree [files-list]
  "Takes a flat list of files as returned by the DatArchive API by using
  the readdir API method and returns a sequence representing a tree, where
  files that point to a directory have a key 'contents' that contains a
  sequence of files in that directory."
  (let [directories (filter #(.isDirectory (% "stat")) files-list)
        files (filter #(not (.isDirectory (% "stat"))) files-list)]
    (concat
      (map
        (fn [directory]
          (assoc directory
                 "contents"
                 (filter
                  (fn [file]
                    (let [dir-name (str (directory "name") "/")
                          index (count dir-name)]
                      (and
                        (.startsWith (file "name") dir-name)
                        (nil? (clojure.string/index-of (file "name") "/" index)))))
                  files)))
        (sort-by #(% "name") directories))
      (sort-by #(% "name") (filter #(not (clojure.string/includes? (% "name") "/")) files)))))

(def colors
  (js->clj jscolors :keywordize-keys true))

(defn browse-daturl [daturl]
  (let [archive (js/DatArchive. daturl)]
    (readdir archive "/"
      (fn [files]
        (swap! app-state assoc :archive archive
                               :files files)))))

(defn format-bytes [bytes]
  (cond (>= bytes (* 1024 1024)) (str (.toFixed (/ bytes (* 1024 1024)) 2) " mb")
        (>= bytes 1024) (str (.toFixed (/ bytes 1024) 2) " kb")
        (< bytes 1024) (str bytes " bytes")))

(defn save-file []
  (let [selected-file (r/cursor app-state [:selected-file])
        selected-file-content (r/cursor app-state [:selected-file-content])
        selected-file-edited (r/cursor app-state [:selected-file-edited])
        changed-files (r/cursor app-state [:changed-files])
        changed (r/cursor app-state [:changed])
        archive (r/cursor app-state [:archive])]
    (when @selected-file-edited
      (swap! changed-files assoc (@selected-file "name") nil)
      (-> (.writeFile ^js @archive (@selected-file "name") @selected-file-edited)
          (.then (fn []
                   (reset! selected-file-edited nil)
                   (reset! changed true)))
          (.catch
           (fn [e]
             (js/console.log "save-file .writeFile error: " e)))))))

;; Material UI Themes

(def light-theme
  (get-theme/default light-base-theme/default))

(def dark-theme-toolbar
  (get-theme/default
    (clj->js
     (merge
       (js->clj dark-base-theme/default :keywordize-keys true)
       {:palette {:primary1Color "#303030"
                  :primary2Color "#ffffff"
                  :primary3Color (:grey600 colors)
                  :accent1Color (:pinkA200 colors)
                  :accent2Color (:grey400 colors)
                  :accent3Color (:pinkA100 colors)
                  :textColor (:fullWhite colors)
                  :secondaryTextColor (:grey700 colors)
                  :canvasColor "#303030"
                  :borderColor (:grey300 colors)
                  :disabledColor (:grey300 colors)
                  :pickerHeaderColor (:grey100 colors)
                  :clockCircleColor (:grey100 colors)
                  :alternateTextColor "#ffffff"}}))))

(def dark-theme
  (get-theme/default
    (clj->js
     (assoc
       (js->clj dark-base-theme/default :keywordize-keys true)
       :palette
       (merge
         (:palette (js->clj dark-base-theme/default :keywordize-keys true))
         {:palette {:primary1Color "#303030"
                    :alternateTextColor (colors :white)}})))))

;; React Components

(defn list-item-file [file]
  "A ListItem React component representing a file or directoy."
  (let [expanded (r/atom false)
        owner (r/cursor app-state [:owner])
        selected-file (r/cursor app-state [:selected-file])
        selected-file-edited (r/cursor app-state [:selected-file-edited])
        changed-files (r/cursor app-state [:changed-files])]
    (fn [file]
      [:> list-item/default
        {:primary-text (file "name")
         :style {:color (when
                          (and @owner
                               (or (@changed-files (file "name"))
                                   (and (= @selected-file file)
                                        @selected-file-edited)))
                          (:blue400 colors))}
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
                               ^{:key (file "name")}
                               [list-item-file file]))))
         :nested-list-style {:margin-left 10}
         :primary-toggles-nested-list (when-not (empty? (file "contents")) true)
         :on-nested-list-toggle (fn [list]
                                  (swap! expanded not))
         :on-click
          (fn [e]
            (when-not (.isDirectory (file "stat"))
              ; preserve unsaved changes before switching files
              (if @selected-file-edited
                (swap! changed-files assoc (get @selected-file "name")
                                           @selected-file-edited))
              ; reset current file editing data
              (swap! app-state assoc :selected-file file
                                     :selected-file-edited nil
                                     :mode nil)
              ; set syntax highlighting if available
              (when-let [index (clojure.string/last-index-of (file "name") ".")]
                (let [file-ending (.toLowerCase (subs (file "name") (+ index 1)))
                      mode (get filetypes-codemirror file-ending)]
                  (when mode
                    (swap! app-state assoc :mode mode))))
              ; read file from Dat archive unless there are unsaved changes
              (if-let [old-edit (@changed-files (file "name"))]
                (swap! app-state assoc :selected-file-content old-edit
                                       :selected-file-edited old-edit)
                (-> (.readFile ^js (:archive @app-state) (file "name"))
                    (.then #(swap! app-state assoc :selected-file-content %))
                    (.catch
                     (fn [e]
                       (js/console.log "list-item-file .readFile error: " e)))))))}])))

(defn files-list []
  "A List React component that can show a list of file ListItems"
  (let [archive (r/cursor app-state [:archive])
        files (r/cursor app-state [:files])]
    (fn []
      ^{:key (if @archive
               (.-url @archive)
               (random-uuid))}
      [:> paper/default
        {:z-depth 1
         :style {:padding 0
                 :margin 10
                 :flex 1
                 :max-width 350
                 :overflow-y "auto"}}
        [:> paper/default
          {:z-depth 0}
          [:> list/default
            (for [file (file-tree @files)]
              ^{:key (file "name")}
              [list-item-file file])]]])))

(defn editor []
  "CodeMirror React component"
  (let [selected-file (r/cursor app-state [:selected-file])
        selected-file-content (r/cursor app-state [:selected-file-content])
        selected-file-edited (r/cursor app-state [:selected-file-edited])
        wrap-lines (r/cursor app-state [:wrap-lines])
        mode (r/cursor app-state [:mode])]
    (fn []
      [:div {:style {:flex 1
                     :display "flex"
                     :flex-direction "column"
                     :overflow "hidden"}}
       [:> paper/default
         {:style {:display "flex"
                  :flex-direction "column"
                  :flex 1
                  :font-size (str (setting-for :font-size) "pt")
                  :padding 0
                  :margin 10
                  :margin-left 0}}
        ^{:key (if @selected-file
                 (@selected-file "name")
                 (random-uuid))}
        [:> code-mirror/UnControlled
          {:value @selected-file-content
           :class-name "react-cm-flex"
           :options {:line-numbers true
                     :line-wrapping @wrap-lines
                     :mode @mode
                     :theme (if (setting-for :dark-theme)
                              "dracula"
                              "default")
                     :extra-keys {"Ctrl-S" #(save-file)
                                  "Cmd-S" #(save-file)}}
           :on-change (fn [editor data value]
                        (if-not (= value @selected-file-content)
                          (reset! selected-file-edited value)))}]]])))

(defn dat-input []
  "A React component that displays a text field and a button that lets you
  enter the URL to and open a Dat Archive."
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
  "A React component that displays the toolbar of the app"
  (let [wrap-lines (r/cursor app-state [:wrap-lines])
        archive (r/cursor app-state [:archive])
        changed (r/cursor app-state [:changed])
        owner (r/cursor app-state [:owner])
        changed-files (r/cursor app-state [:changed-files])
        selected-file-edited (r/cursor app-state [:selected-file-edited])]
    (fn []
      [:> paper/default
        {:z-depth 1
         :style {:margin 10
                 :margin-bottom 0
                 :padding 0}}
        [:> toolbar/default
         {:style (when (setting-for :dark-theme)
                   {:background-color (:grey800 colors)})}
         [:> toolbar-group/default {:first-child true}
          [:> button/default
            {:on-click (fn []
                         (select-archive
                           #(swap! app-state assoc :files %)
                           :recursive? true))
             :background-color (:blue500 colors)
             :style {:min-width 160}}
            "Open Dat archive"]
          (if-not @owner
            [:> toolbar-title/default {:text "read-only"}])]
         [:> toolbar-group/default
          [:> button/default
            {:background-color (:blue500 colors)
             :title "Save (Ctrl+S)"
             :disabled (or
                         (not @owner)
                         (not @selected-file-edited))
             :on-click save-file}
            "Save"]
          [:> button/default
            {:background-color (:blue500 colors)
             :on-click (fn []
                         (reset! changed false)
                         (.commit ^js @archive))
             :disabled (or
                        (not @owner)
                        (not @changed))}
            "Publish"]
          [:div
           [:> toggle/default
             {:label "Wrap lines"
              :thumb-switched-style (when (setting-for :dark-theme)
                                      {:background-color (:cyan500 colors)})
              :track-switched-style (when (setting-for :dark-theme)
                                      {:background-color (:cyan800 colors)})
              :toggled @wrap-lines
              :on-toggle (fn [event checked]
                           (reset! wrap-lines checked))}]]]]])))

(defn app-drawer []
  "A React component that display a drawer with settings that can be
  expanded by clicking on the burger menu in the AppBar component"
  (let [settings (r/cursor app-state [:settings])
        drawer-open (r/cursor app-state [:drawer-open])]
    (fn []
      [:> drawer/default
       {:open @drawer-open}
       [:div {:style {:padding 10}}
        [:p "Font size: " (setting-for :font-size)]
        [:> slider/default
         {:default-value 12
          :min 8
          :max 40
          :step 1
          :on-change (fn [e value]
                       (swap! settings assoc :font-size value)
                       (save-settings)
                       ; workaround to refresh font-size in CodeMirror
                       (let [old-file (:selected-file @app-state)]
                         (swap! app-state assoc :selected-file nil)
                         (js/setTimeout
                          (fn []
                            (swap! app-state assoc :selected-file old-file)
                            (r/force-update-all))
                          500)))}]]
       [:> list/default
        [:> list-item/default
         {:primary-text "Dark theme"
          :right-toggle
          (r/as-element
            [:> toggle/default
             {:toggled (setting-for :dark-theme)
              :on-toggle (fn [e toggled]
                           (swap! settings assoc :dark-theme toggled)
                           (save-settings)
                           (r/after-render
                             (fn []
                               (r/force-update-all))))}])}]]])))

(defn content []
  "The root React component of the app"
  (let [drawer-open (r/cursor app-state [:drawer-open])]
    (fn []
      [:div {:style {:width "100%"
                     :height "100%"
                     :background-color (when (setting-for :dark-theme)
                                         "#303030")}}
        [:> theme-provider/default {:mui-theme (if (setting-for :dark-theme)
                                                 dark-theme
                                                 light-theme)}
          [:div
           [app-drawer]
           [:div {:id "app-container"
                  :style {:display "flex"
                          :flex-direction "column"
                          :position "absolute"
                          :top 0
                          :left (if @drawer-open 256 0)
                          :right 0
                          :bottom 0
                          :background (when (setting-for :dark-theme)
                                        "#303030")}}
             (if (setting-for :dark-theme)
               [:> theme-provider/default {:mui-theme dark-theme-toolbar}
                [:div
                 [:> appbar/default
                  {:title "Dat File Editor"
                   :on-left-icon-button-click #(swap! drawer-open not)}]
                 [app-toolbar]]]
               [:div
                [:> appbar/default
                 {:title "Dat File Editor"
                  :on-left-icon-button-click #(swap! drawer-open not)}]
                [app-toolbar]])
             [:div
               {:class "twopane"
                :style {:display "flex"
                        :flex-direction "row"
                        :flex 1}}
               [files-list]
               [editor]]]]]])))

;; App Lifecycle methods

(defn start []
  ;; start is called by init and after code reloading finishes
  ;; this is controlled by the :after-load in the config

  (load-settings)

  (r/render [content]
            (js/document.getElementById "app")))

(defn ^:export init []
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds

  (let [location (if (= js/window.location.hostname "localhost")
                   "dat://ddb2c76c69d3245c95ba77c29b7c5f206a60f30cb3c69bd8c2389a74429c1f23"
                   (.toString js/window.location))]
    (browse-daturl location))

  (start))

(defn stop [])
  ;; stop is called before any code is reloaded
  ;; this is controlled by :before-load in the config
