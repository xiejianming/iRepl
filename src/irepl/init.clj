;   Copyright (c) xjm (xiejianming@gmail.com). All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns irepl.init
  (:use [dbkid])
  (:use [irepl.utils])
  (:use [clojure.java.io :only [file]])
  (:use [clojure.string :only [split-lines]]))

(def ^:dynamic *builtins* (atom nil))
(def ^:dynamic *irepl_attr* (atom nil))
(def ^:dynamic *shell_env* (atom nil))

;;<<<<<<<<<< Working Directory <<<<<<<<<<
(def ^:dynamic *wd* (atom nil))
(defn set-wd 
  [^String path] 
  (reset! *wd* (file path)))
(defn init-wd [] (set-wd (@*irepl_attr* :home)))
(defn get-parent-path[] (.getParent @*wd*))
(defn get-current-path[] (.getCanonicalPath @*wd*))
(defn pwd 
  "Show current working directory."
  [& _] 
  (println (.getCanonicalPath @*wd*)))
(defn pwd-p 
  "Show parent directory of current directory."
  [& _] 
  (let [parent (.getParent @*wd*)]
    (if (nil? parent)
      (println "You're in the top directory already.")
      (println parent))))

;;>>>>>>>>>> Working Directory >>>>>>>>>>

(defn- sh
  "Execute cmd as external(system call) program."
  [^String cmd]
  (eval `(clojure.java.shell/sh ~@(break-str cmd)
                                :dir ~(get-current-path)
                                :env ~(deref *shell_env*))))

(defn exec-external
  [^String cmd]
  (let [cmd (case (:os @*irepl_attr*)
              "windows" (str "cmd /c " cmd)
              "others")]
    (sh cmd)))

(defn do-external
  [^String cmd]
  (let [cmd (case (:os @*irepl_attr*)
              "windows" (str "cmd /c start cmd /c \"" cmd "&echo.&pause\"")
              "others")]
    (sh cmd)))

(defn prnt-external-result
  "Print external execution result."
  [r]
  (let [{:keys [exit out err]} r]
    (if (not= out "") (println out))
    (if (not= err "") (println err))))

#_(def do-external
  "Execute external cmd and print result."
  (comp prnt-external-result exec-external))

(defn ihelp
  "iRepl help function."
  [^String s]
  (if (empty? s)
    (do
      (println "Please type a \"?\" to start....")
      (println "  ? [xxx]   - Show this message or doc info of function/cmd \"xxx\".")
      (println "  ??        - Show all available iRepl commands.")
      (println "  ! [xxx]   - Open a cmd window or execute external cmd \"xxx\" in a popup window.")
      (println "  q         - Quit iRepl."))  
    (case s
      "*" (clojure.pprint/print-table 
            [:name :desc]
            (loop [lst (sort @*builtins*) rows []]
              (if (empty? lst)
                (conj rows                      
                      {:name "$${xxx}"
                       :desc "Extraction operator to extract (a String) result from OS/Clojure call."}
                      {:name "Quit" 
                       :desc "Type one of these to quit iRepl: 8/88/quit/q/Q/bye"})
                (let [[k v] (first lst)]
                  (dbk k v)
                  (recur (rest lst)
                         (conj rows
                               {:name k
                                :desc (first (split-lines (or (:doc (meta v)) "")))}))))))             
      (let [btin (@*builtins* s)]
        (if btin 
          (println (:doc (meta (@*builtins* s)))) ;; check iRepl command first
          (let [doc? (:doc (meta (resolve (symbol s))))]  ;; then check clojure functions
            (if doc?
              (eval (list 'doc (symbol s)))  ;; and finally check external commands
              (prnt-external-result (sh (str "help " s))))))))))

(defn iihelp
  "Show all available iRepl commands."
  [^String s]
  (ihelp "*"))

(defmacro +internal
  "Register a function/macro as iRepl internal cmd."
  [name, f]
  `(plus *builtins* (name '~name) #'~f))
(defmacro -internal
  "Remove registered internal cmd."
  [name]
  `(minus *builtins* (name '~name)))

(defmacro +attr
  "Add an env attribute."
  [k, v]
  `(plus *irepl_attr* ~k ~v))
(defmacro -attr
  "Remove an env attribute."
  [k]
  `(minus *irepl_attr* ~k))

(defn +env
  "Add an env variable."
  [^String vname ^String value]
  (plus *shell_env* vname value))
(defn -env
  "Remove an env variable."
  [^String vname]
  (minus *shell_env* vname))

(defn init-common
  "Init env and load cmd for common use."
  []
  (reset! *irepl_attr* {})
  (+attr :os (get-os-name))
  (+attr :home (System/getProperty "user.home"))
  (+attr :dir (System/getProperty "user.dir"))
  (init-wd)
  
  (reset! *builtins* {})
  (+internal . pwd)
  (+internal .. pwd-p)
  (+internal pwd pwd)
  (+internal ? ihelp)
  (+internal ?? iihelp)
  (+internal help ihelp)
  (+internal shutup db)
  
  nil)

(defn init-windows
  "Init env and load cmds for Windows."
  []
  (dbk "Loading cmds for Windows...")
  (use 'irepl.windows.core :reload))

(defn init-linux
  "Init env and load cmds for Linux."
  []
  (dbk "Loading cmds for Linux..."))

(defn init 
  "Initialize."
  [& opts]
  (init-common)
  (case (:os @*irepl_attr*)
    "windows" (init-windows)
    "linux" (init-linux)
    (println "what the hell do u want?")))

(defn clean-up
  "Clean up builtins & env while exiting iREPL."
  []
  (reset! *builtins* nil)
  (reset! *irepl_attr* nil)
  (reset! *shell_env* nil)
  (reset! *wd* nil))


