(ns otoch.constraints
  (:require [otoch.line :as line]
            [otoch.consts :as consts]))

(defn constrain-pos [constrain-fn platforms old-pos new-pos]
  (reduce
   (fn [pos {:keys [passable? platform-pos]}]
     (constrain-fn passable? platform-pos old-pos pos))
   new-pos platforms))

(defn platform-constrain [pass? pos old-pos new-pos]
  (line/constrain-offset
   {:passable? pass?
    :h-edge consts/h-edge
    :v-edge consts/v-edge
    :minus-h-edge consts/minus-h-edge
    :minus-v-edge consts/minus-v-edge}
   pos new-pos old-pos))

(defn dynamite-constrain [pass? pos old-pos new-pos]
  (line/constrain-offset
   {:passable? pass?
    :h-edge 0.1
    :v-edge 0.3
    :minus-h-edge 0.9
    :minus-v-edge 0.5}
   pos new-pos old-pos))

(defn enemy-constrain [pass? pos old-pos new-pos]
  (line/constrain-offset
   {:passable? pass?
    :h-edge 0.3
    :v-edge 0.3
    :minus-h-edge 0.7
    :minus-v-edge 0.5}
   pos new-pos old-pos))

(defn particle-constrain [pass? pos old-pos new-pos]
  (line/constrain-offset
   {:passable? pass?
    :h-edge 0.01
    :v-edge 0.01
    :minus-h-edge 0.99
    :minus-v-edge 0.99}
   pos new-pos old-pos))
