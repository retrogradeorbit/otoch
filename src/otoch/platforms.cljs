(ns otoch.platforms
  (:require [infinitelives.utils.vec2 :as vec2]
            [otoch.map :as tm]))

(def platform-map
  (->
   [
    "    "
    " ---"]
   tm/strs->keymap  tm/randomise-keymap tm/remaph-keymap
   ))

(def platform2-map
  (->
   [
    "    "
    " BBB"]
   tm/strs->keymap  tm/randomise-keymap tm/remaph-keymap))

(defn not-passable? [x y]
  (tm/not-passable? (tm/get-tile-at tm/tile-map x y)))

(def passable? (comp not not-passable?))

(defn not-platform-passable? [x y]
  (tm/not-passable? (tm/get-tile-at platform-map x y)))

(def platform-passable? (comp not not-platform-passable?))

(defn not-platform2-passable? [x y]
  (tm/not-passable? (tm/get-tile-at platform2-map x y)))

(def platform2-passable? (comp not not-platform2-passable?))

(defn walkable? [x y]
  (tm/walkable? (tm/get-tile-at tm/tile-map x y)))


(def platforms
  [{:name :level
    :fn (fn [_] (vec2/zero))
    :passable? walkable?
    :apply? (fn [_] true)}

   {:name :t-platform
    :fn (fn [fnum]
          (let [x 9
                y (+ 7 5 (* 2.01 (Math/sin (/ fnum 60))))]
            (vec2/vec2 (/ (int (* 64 x)) 64) (/ (int (* 64 y)) 64))))
    :passable? platform-passable?
    :apply? (fn [pos] (let [x (vec2/get-x pos)
                            y (vec2/get-y pos)]
                        (and
                         (<= 8 x 15)
                         (<= 9 y 19))))}

   {:name :platform-cavern-1
    :fn (fn [fnum]
          (let [x 22
                y (+ 34 5 (* 2.01 (Math/sin (/ fnum 60))))]
            (vec2/vec2 (/ (int (* 64 x)) 64) (/ (int (* 64 y)) 64))))
    :passable? platform-passable?
    :apply? (fn [pos] (let [x (vec2/get-x pos)
                            y (vec2/get-y pos)]
                        (and
                         (<= 20 x 27)
                         (<= 25 y 55))))}

   {:name :platform-cavern-2
    :fn (fn [fnum]
          (let [x (+ 35 (* 7.01 (Math/sin (/ fnum 80))))
                y (+ 32 5)]
            (vec2/vec2 (/ (int (* 64 x)) 64) (/ (int (* 64 y)) 64))))
    :passable? platform-passable?
    :apply? (fn [pos] (let [x (vec2/get-x pos)
                            y (vec2/get-y pos)]
                        (and
                         (<= 25 x 50)
                         (<= 35 y 40))))}

   {:name :platform-cavern-3
    :fn (fn [fnum]
          (let [x (+ 56 (* 7.01 (- (Math/sin (/ fnum 80)))))
                y (+ 32 5)]
            (vec2/vec2 (/ (int (* 64 x)) 64) (/ (int (* 64 y)) 64))))
    :passable? platform-passable?
    :apply? (fn [pos] (let [x (vec2/get-x pos)
                            y (vec2/get-y pos)]
                        (and
                         (<= 50 x 70)
                         (<= 35 y 40))))}

   {:name :platform-cavern-4
    :fn (fn [fnum]
          (let [x (+ 55 (* -4.01 (Math/sin (/ fnum 80))))
                y (+ 22 5 (* 7.01 (Math/cos (/ fnum 80))))]
            (vec2/vec2 (/ (int (* 64 x)) 64) (/ (int (* 64 y)) 64))))
    :passable? platform-passable?
    :apply? (fn [pos] (let [x (vec2/get-x pos)
                            y (vec2/get-y pos)]
                        (and
                         (<= 50 x 70)
                         (<= 20 y 40))))}])


(defn prepare-platforms [platforms fnum]
  (->> platforms
       (mapv #(let [platform-pos ((:fn %) fnum)
                   old-platform-pos ((:fn %) (dec fnum))]
               (assoc %
                      :platform-pos platform-pos
                      :old-platform-pos old-platform-pos
                      :platform-delta (vec2/sub platform-pos old-platform-pos))))))

(defn filter-platforms [platforms pos]
  (->> platforms
       (filterv #((:apply? %) pos))))
