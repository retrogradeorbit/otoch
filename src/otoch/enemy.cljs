(ns otoch.enemy
  (:require [infinitelives.utils.vec2 :as vec2]
            [infinitelives.utils.events :as e]
            [infinitelives.utils.boid :as b]
            [infinitelives.utils.math :as math]
            [infinitelives.utils.spatial :as spatial]
            [infinitelives.utils.sound :as sound]
            [infinitelives.utils.async :as async]
            [infinitelives.utils.console :refer [log]]
            [infinitelives.pixi.sprite :as s]
            [infinitelives.pixi.canvas :as c]
            [otoch.state :as state]
            [otoch.consts :as consts]
            [otoch.constraints :as constraints]
            [otoch.platforms :as platforms]
            [cljs.core.async :refer [<! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defonce enemies (atom {}))

#_ [@enemies @state/state]

(defn add! [ekey enemy]
  ;;(js/console.log "add!" ekey (str enemy))
  (swap! enemies assoc ekey enemy))

(defn remove! [ekey]
  ;;(js/console.log "remove!" ekey)
  (swap! enemies dissoc ekey))

(defn count-enemies []
  (count @enemies))

(defn collided?
  "given the players pos, find the first collided enemy"
  [player-pos]
  (let [coll (->>
              (for [[k {:keys [pos]}] @enemies]
                (let [distance-squared (vec2/magnitude-squared (vec2/sub player-pos pos))]
                  (when (< distance-squared 0.5)
                    k)))
              (filter identity)
              first)]
    coll
    ))

(def reverse-dir
  {:left :right
   :right :left})

(def speed 0.0005)

(defn wobble [y n]
  (+ y (* 4 (Math/sin (/ n 12))))
  )

(defn spawn [container start-pos sprite]
  (let [start-game-num (:game-num @state/state)
        ekey (keyword (gensym))]
    (go
      (async/continue-while
       (= start-game-num (:game-num @state/state))
       (let [start-frame 0]
         (c/with-sprite container
           [enemy (s/make-sprite sprite :pos (vec2/scale start-pos 64))]
           (add! ekey {:pos start-pos} )
           (loop [n 0
                  p start-pos
                  v (vec2/zero)
                  facing :left
                  ]
             (let [ppos (vec2/scale p 64)
                   [x y] ppos
                   y (if (= sprite :enemy-1) (wobble y n) y)
                   ]
               (swap! enemies assoc-in [ekey :pos] p)
               (s/set-pos! enemy x y))

             ;; stop processing when far away rom player
             #_ (while (>
                     (vec2/magnitude-squared
                      (vec2/sub (:pos @state/state) p))
                     (* 50 50))
               ;; far away from player. sleep for a bit (less CPU)
               (<! (e/wait-time (int (+ 1000 (* 1000 (rand)))))))

             (s/set-scale! enemy (if (= :left facing) 1 -1) 1)
             (<! (e/next-frame))
             (if true ;;(< n 3000)
               ;; still alive
               (do
                 #_ (let [frame (int (/ n 60))
                          texture :rune-1]
                      (s/set-texture! sprite (t/get-texture texture)))

                 (let [platform-state
                       (-> platforms/platforms
                           (platforms/prepare-platforms n)
                           (platforms/filter-platforms (vec2/zero)))

                       new-pos
                       (constraints/constrain-pos
                        constraints/enemy-constrain
                        (-> platforms/platforms
                            (platforms/prepare-platforms (+ 1 start-frame n))
                            (platforms/filter-platforms p))
                        p (vec2/add (vec2/add p v) (vec2/vec2 (if (= :left facing) (- speed) speed) 0)))

                       new-vel (-> new-pos
                                   (vec2/sub p)

                                   (vec2/add consts/gravity)
                                   (vec2/scale 0.98))
                       vel-x (vec2/get-x new-vel)
                       new-facing (if (zero? vel-x) (reverse-dir facing) facing)
                       ]

                   (recur (inc n)
                          new-pos
                          new-vel
                          new-facing)))
               p)))))
      (remove! ekey))))
