(ns otoch.state
  (:require [infinitelives.utils.vec2 :as vec2]))

(def initial-state
  {:pos (vec2/vec2 0 0)
   :touched-heart? false
   :runes 0
   :trees 0

   ;; we inc this every time a game is finished
   ;; and canuse this to early-exit go blocks
   :game-num 0})

(defonce state
  (atom initial-state))

(defn reset-state! []
  (reset! state initial-state))


#_ (swap! state update :game-num inc)
