(ns rocks.pho.eth.whac-a-mole-eth
  (:gen-class)
  (:import [rocks.pho.eth.utils WebSocket]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!")
  (WebSocket/executeWebSocket))
