(ns otoch.heart
  (:require [infinitelives.utils.vec2 :as vec2]
            [infinitelives.utils.events :as e]
            [infinitelives.utils.boid :as b]
            [infinitelives.utils.math :as math]
            [infinitelives.utils.async :as async]
            [infinitelives.utils.spatial :as spatial]
            [infinitelives.utils.sound :as sound]
            [infinitelives.utils.console :refer [log]]
            [infinitelives.pixi.sprite :as s]
            [infinitelives.pixi.canvas :as c]
            [otoch.state :as state]
            [otoch.consts :as consts]
            [otoch.map :as tm]
            [otoch.constraints :as constraints]
            [otoch.platforms :as platforms]
            [otoch.particle :as particle]
            [cljs.core.async :refer [<! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn sin [amp freq n]
  (* amp (Math/sin (* n freq))))

(def x-amp (/ 7 64))
(def x-freq (/ 7 200))
(def y-amp (/ 10 64))
(def y-freq (/ 11 200))
(def size-amp 0.2)
(def size-freq 0.02)
(def size-off 1)
(def star-burst-speed 0.05)
(def star-burst-life 300)
(def star-at-a-time 3)
(def star-every-frame 60)

;; what percentage of runes brings the otoch to jumping distance
(def precent-runes 0.9)

(defn star-burst-texture-fn []
  (rand-nth
   [:star-1  :star-2  :star-3  :star-4
    :star-5  :star-6  :star-7  :star-8
    :star-9  :star-10 :star-11 :star-12
    :star-13 :star-14 :star-15 :star-16
    ]))

(defn star-burst-vel []
  (-> (vec2/random-unit)
      (vec2/scale star-burst-speed)
      (vec2/add (vec2/vec2 0 -0.1))
      ))

(defn lower-vec []
  (vec2/vec2 0 (min 8 (* 8 (/ (:trees @state/state) (* 0.9 tm/num-runes)))))
  )

#_ (lower-vec)

(defn spawn [container]
  (let [start-game-num (:game-num @state/state)]
    (async/go-while
     (= start-game-num (:game-num @state/state))
     (c/with-sprite container
       [heart (s/make-sprite :heart :pos (vec2/scale tm/heart-position 64))]
       (loop [n 0]
         (let [[x y] tm/heart-position]
           (s/set-pos! heart
                       (vec2/scale
                        (vec2/add
                         (vec2/vec2
                          (+ x (sin x-amp x-freq n))
                          (+ y (sin y-amp y-freq n)))
                         (lower-vec))
                        64))
           #_ (s/set-scale! heart (+ size-off (sin size-amp size-freq n))))
         (<! (e/next-frame))

         (let [player-pos (:pos @state/state)
               distance-squared
               (vec2/magnitude-squared
                (vec2/sub player-pos
                          (vec2/add tm/heart-position (lower-vec))))]
           (if (> distance-squared 1)
             ;; still hanging around
             (recur (inc n))

             ;; player has touched us
             (do
               (swap! state/state assoc :touched-heart? true)

               (loop [n n]
                 (let [[x y] tm/heart-position]
                   (s/set-pos! heart
                               (vec2/scale
                                (vec2/add
                                 (vec2/vec2
                                  (+ x (sin x-amp x-freq n))
                                  (+ y (sin y-amp y-freq n)))
                                 (lower-vec))
                                64))
                   #_ (s/set-scale! heart (+ size-off (sin size-amp size-freq n))))

                 ;; spawn a particle
                 (when (zero? (mod n star-every-frame))
                   (doseq [num (range star-at-a-time)]
                     (particle/spawn
                      container
                      (star-burst-texture-fn)
                      n
                      (vec2/add tm/heart-position (lower-vec))
                      (star-burst-vel)
                      (rand)
                      star-burst-life
                      )))

                 (<! (e/next-frame))

                 (recur (inc n)))))))))))
