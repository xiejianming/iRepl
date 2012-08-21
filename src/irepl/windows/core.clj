;   Copyright (c) xjm (xiejianming@gmail.com). All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns irepl.windows.core
  (:use [clojure.java.io :only [file]])
  (:use [irepl.utils])
  (:use [irepl.init]))

;;<<<<<<<<<< system shell env <<<<<<<<<<
(reset! *shell_env* {})
(defn get-shell-env
  "Get shell env variables."
  []
  (let [env (drop-last 2 (.split 
                         (:out (apply clojure.java.shell/sh (break-str "cmd /k set"))) 
                         "\r\n"))]
    (reduce #(conj %1 (vec (.split %2 "="))) {} env)))
    
; get & set shell env
(reset! *shell_env* (get-shell-env))
(+attr :env *shell_env*)


;;>>>>>>>>>> system shell env >>>>>>>>>>
(defn cd 
  "Change directory."
  [^String opts]
  (case opts
    "" (pwd)
    "/" (do (set-wd (subs (get-current-path) 0 3)) (pwd))
    "\\" (do (set-wd (subs (get-current-path) 0 3)) (pwd))
    (let [tmp-f (file (str (get-current-path) "/" opts))]
      (gdb 1 tmp-f)
      (if (.isDirectory tmp-f)
        (set-wd (.getCanonicalPath tmp-f))
        (let [tmp-f (file opts)]
          (gdb 2 tmp-f)
          (if (.isDirectory tmp-f)
            (set-wd (.getCanonicalPath tmp-f))
            (println "神马目录名啊，路径不存在！: " opts))))
      (pwd))))

(+internal cd cd)

(defn ls
  "List all files under current directory."
  [^String opts]
  (exec-external (construct-str-cmd "cmd /k dir" (break-str opts))))

(+internal ls ls)
(+internal dir ls)