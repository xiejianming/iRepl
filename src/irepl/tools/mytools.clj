(ns irepl.tools.mytools
  (:use [clojure.repl])
  (:use [dbkid])
  (:use [irepl.utils])
  (:use [irepl.init]))

(defn- my-pst
  "Prints a stack trace of the exception."
  [& _]
  (pst))

(+internal pst my-pst)

(defn- sys
  "A call to System/getProperties or System/getProperty.
Depends on given option, this tool will return a set of system properties, or 
   print a (sorted) list of keys of system properties, etc.."
  [^String o]
  (let [pm (into {} (System/getProperties))]
    (if (empty? o)
      (width-prn (keys pm))
      (println
        (reduce #(str %1 \newline %2 ": " (pm %2))
                "--------------------------------------------------------------------------------"
                (break-str o))
                "\n--------------------------------------------------------------------------------"))))

(+internal sys sys)