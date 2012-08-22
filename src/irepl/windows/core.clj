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
(defn- get-shell-env
  "Get shell env variables."
  []
  (let [env (trim-n-break-cmd-output  
              (:out (clojure.java.shell/sh "cmd" "/k" "set")))]
    (reduce #(conj %1 (vec (.split %2 "="))) {} env)))

(reset! *shell_env* (get-shell-env))

(defn cmd-set
  "To display/set/unset env variables(a mimic set command in cmd.exe).
   Usage: set [name[=[value]]]
     - a 'set' without any parameter will print all env variables;
     - 'set name' will print variables(with their values) with their names contain 'name';
     - 'set name=' will unset the variable;
     - 'set name=value' will set env variable 'name', in which:
           - any form of '%v-name%' will be replace by their value respectively.
   Note: any spaces on the right most will also be treated as part of variable value."
  [^String opts]
  (if (empty? (.trim opts))
    (loop [m (sort @*shell_env*)]
      (if m
        (do
          (let [[k v] (first m)]
            (println (str k "=" v)))
          (recur (next m)))))
    (let [idx (.indexOf opts "=")]
      (gdb idx)
      (if (= idx -1)
        (let [s (filter #(> (.indexOf (first %) opts) -1) @*shell_env*)]          
          (if (empty? s)
            (println (str "Environment variable " opts " not defined."))
            (loop [ss s]
              (if-let [[[k v]] ss]
                (do
                  (gdb k)
                  (println (str k "=" v))
                  (recur (next ss)))))))
        (let [name (subs opts 0 idx)
              value (subs opts (inc idx))]
          (if (empty? value)
            (-env name)
            (+env name
                  (clojure.string/replace value 
                                          #"%\w+%" 
                                          #(let [k (subs %1 1 (dec (count %1)))]
                                             (@*shell_env* k ""))))))))))
    
(+internal set cmd-set)

;;>>>>>>>>>> system shell env >>>>>>>>>>
               

(defn cd 
  "Change directory.
   Usage: cd [/] [\\] [path] [relative-path]
     - 'cd' without any parameters will print current directory;
     - 'cd /' or 'cd \\' will change to root of coresponding drive;
     - 'cd path' will change to given path;
     - 'cd relative-path' will change to relative path.     
   If the given path is not correct, an error msg will be printed."
  [^String opts]
  (let [p (.trim opts)]
    (case p
      "" (pwd)
      "/" (do (set-wd (subs (get-current-path) 0 3)) (pwd))
      "\\" (do (set-wd (subs (get-current-path) 0 3)) (pwd))
      (let [tmp-f (file (str (get-current-path) "/" p))]
        (gdb 1 tmp-f)
        (if (.isDirectory tmp-f)
          (set-wd (.getCanonicalPath tmp-f))
          (let [tmp-f (file p)]
            (gdb 2 tmp-f)
            (if (.isDirectory tmp-f)
              (set-wd (.getCanonicalPath tmp-f))
              (println "Incorrect path name: " p))))
        (pwd)))))

(+internal cd cd)

(defn ls
  "List all files under current directory."
  [^String opts]
  (do-external 
    (construct-str-cmd "cmd /k dir" (break-str opts))))

(+internal ls ls)
(+internal dir ls)