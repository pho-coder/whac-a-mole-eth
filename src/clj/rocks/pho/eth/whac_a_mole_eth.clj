(ns rocks.pho.eth.whac-a-mole-eth
  (:gen-class)
  (:require [rocks.pho.eth.watcher :as watcher]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!")
  (watcher/watch-depth))
