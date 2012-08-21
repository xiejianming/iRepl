;   Copyright (c) xjm (xiejianming@gmail.com). All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns irepl.init
  (:use [irepl.utils])
  (:use [clojure.java.io :only [file]]))

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
(defn pwd [& _] (println (.getCanonicalPath @*wd*)))
(defn pwd-p [& _] 
  (let [parent (.getParent @*wd*)]
    (if (nil? parent)
      (println "You're in the top directory already.")
      (println parent))))

;;>>>>>>>>>> Working Directory >>>>>>>>>>

(defmacro +internal
  "Register a function/macro as iREPL internal cmd."
  [^String name, f]
  `(add *builtins* (name '~name) #'~f))

(defmacro +attr
  "Add an env attribute."
  [k, v]
  `(add *irepl_attr* ~k ~v))

(defn +env
  "Add env setting."
  [vname value]
  (add *shell_env* vname value))

(defn init-common
  "Init env and load cmd for common use."
  []
  (reset! *builtins* {})
  (reset! *irepl_attr* {})
  (+attr :os (get-os-name))
  (+attr :home (System/getProperty "user.home"))
  (+attr :dir (System/getProperty "user.dir"))
  (init-wd)
  
  (+internal . pwd)
  (+internal .. pwd-p)
  (+internal pwd pwd)
  nil)

(defn init-windows
  "Init env and load cmds for Windows."
  []
  (gdb "Loading cmds for Windows...")
  (use 'irepl.windows.core :reload))

(defn init-linux
  "Init env and load cmds for Linux."
  []
  (gdb "Loading cmds for Linux..."))

(defn init 
  "Initialize."
  [& opts]
  (init-common)
  (case (@*irepl_attr* :os)
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

(defn exec-external
  "Execute cmd as external(system call) program and print the outcomes."
  [^String cmd]
  (let [[name & opts] (break-str cmd)]
    ;(gdb "====EXTERNAL====")
    (try
      (let [{:keys [exit out err]} (eval `(clojure.java.shell/sh ~@(break-str cmd)
                                                                 :dir ~(get-current-path)))]
        (if (not= out "") (println out))
        (if (not= err "") (println err)))
      (catch Exception e
        (println (.getMessage e))))))