(ns otoch.titlescreen
  (:require [infinitelives.pixi.sprite :as s]
            [infinitelives.pixi.canvas :as c]
            [infinitelives.pixi.resources :as r]
            [infinitelives.pixi.texture :as t]
            [infinitelives.pixi.pixelfont :as pf]
            [infinitelives.utils.events :as e]
            [infinitelives.utils.sound :as sound]
            [infinitelives.utils.vec2 :as vec2]
            [infinitelives.utils.async :as async]
            [otoch.controls :as controls]
            [otoch.map :as tm]
            [cljs.core.async :refer [timeout]])
  (:require-macros [cljs.core.async.macros :refer [go]])
  )

(defn sin [amp freq n]
  (* amp (Math/sin (* n freq))))

(def freq 0.04)

(def font-scale 2)

(def text-title-colour 0xff7f2a)
(def text-main-colour 0xffffff)

(def text-ypos 230)
(def text-spacing 30)
(def volume 0.15)
(def text-delay 600)

;;
;; titlescreen tilemaps
;;
(defn key-for [c]
  (case c
    "l" :dirt-top-left
    "1" :dirt-top-1
    "2" :dirt-top-2
    "3" :dirt-top-3
    "r" :dirt-top-right
    "P" :player
    "B" :block
    "a" :dirt-bottom-1
    "b" :dirt-bottom-2
    ))

(defn strs->keymap [strs]
  (mapv #(mapv key-for %) strs))

(defn make-tile-set [resource-key]
  (let [texture (r/get-texture resource-key :nearest)
        tile-lookup
        {
         :dirt-top-left [(* 3 128) 0]
         :dirt-top-1 [(* 4 128) 0]
         :dirt-top-2 [(* 5 128) 0]
         :dirt-top-3 [(* 6 128) 0]
         :dirt-top-right [(* 7 128) 0]

         :dirt-bottom-1 [768 128]
         :dirt-bottom-2 [(+ 128 768) 128]

         :player [512 384]
         :block [641 384]
         }
        ]
    (->> tile-lookup
         (map (fn [[c pos]] [c (t/sub-texture texture pos [128 128])]))
         (into {})))
  )

(def titlescreen-mapsrc-1
  [
   "l1322321r"
   "abaaabbab"])

(def titlescreen-map-1
  (-> titlescreen-mapsrc-1 strs->keymap tm/remaph-keymap tm/remapv-keymap))

(def titlescreen-mapsrc-3
  [
   "P"
   "B"])

(def titlescreen-map-3
  (-> titlescreen-mapsrc-3 strs->keymap tm/remap-keymap tm/remaph-keymap tm/remapv-keymap))


(defn keyboard-controls [canvas title a1 a2 b1 b2 c1 c2]
  (async/go-while
   (not (controls/start-game?))
   (c/with-sprite canvas :ui
     [text (pf/make-text :pixel title
                         :scale font-scale
                         :x 0 :y text-ypos
                         :tint text-title-colour
                         :visible false)
      text-2 (pf/make-text :pixel a1
                           :scale font-scale
                           :x -200 :y (+ 20 text-spacing text-ypos)
                           :tint text-main-colour
                           :visible false)
      text-3 (pf/make-text :pixel b1
                           :scale font-scale
                           :x -200 :y (+ 20 text-spacing text-spacing text-ypos)
                           :tint text-main-colour
                           :visible false)
      text-4 (pf/make-text :pixel a2
                           :scale font-scale
                           :x 150 :y (+ 20 text-spacing text-ypos)
                           :tint text-main-colour
                           :visible false)
      text-5 (pf/make-text :pixel b2
                           :scale font-scale
                           :x 150 :y (+ 20 text-spacing text-spacing text-ypos)
                           :tint text-main-colour
                           :visible false)

      text-6 (pf/make-text :pixel c1
                           :scale font-scale
                           :x -200 :y (+ 20 text-spacing text-spacing text-spacing text-ypos)
                           :tint text-main-colour
                           :visible false)
      text-7 (pf/make-text :pixel c2
                           :scale font-scale
                           :x 150 :y (+ 20 text-spacing text-spacing text-spacing text-ypos)
                           :tint text-main-colour
                           :visible false)

      ]
     (loop [f 0]
       (when (= f 60) (sound/play-sound :thud volume false) (s/set-visible! text true))
       (when (= f 80) (sound/play-sound :thud volume false) (s/set-visible! text-2 true))
       (when (= f 100) (sound/play-sound :thud volume false) (s/set-visible! text-4 true))
       (when (= f 120) (sound/play-sound :thud volume false) (s/set-visible! text-3 true))
       (when (= f 140) (sound/play-sound :thud volume false) (s/set-visible! text-5 true))
       (when (= f 160) (sound/play-sound :thud volume false) (s/set-visible! text-6 true))
       (when (= f 180) (sound/play-sound :thud volume false) (s/set-visible! text-7 true))
       (<! (e/next-frame))
       (when (< f text-delay)
         (recur (inc f))))

     (sound/play-sound :jump volume false)
     (loop [f 0]
       (s/set-x! text-2 (- -200 (Math/pow 1.2 f)))
       (s/set-x! text-3 (- -200 (Math/pow 1.2 f)))
       (s/set-x! text-4 (+ 150 (Math/pow 1.2 f)))
       (s/set-x! text-5 (+ 150 (Math/pow 1.2 f)))
       (s/set-x! text-6 (- -200 (Math/pow 1.2 f)))
       (s/set-x! text-7 (+ 150 (Math/pow 1.2 f)))

       (<! (e/next-frame))
       (when (< f 40)
         (recur (inc f))))

     (sound/play-sound :jump volume false)
     (loop [f 0]
       (s/set-y! text (+ text-ypos (Math/pow 1.2 f)))
       (<! (e/next-frame))
       (when (< f 40)
         (recur (inc f)))))))

(defn make-tiles [tile-set tile-map]
  (filter identity
   (for [row (range (count tile-map))
         col (range (count (first tile-map)))]
     (let [char (nth (tile-map row) col)]
       (when (not= :space char)
         (js/console.log "!" char)
         (s/make-sprite (tile-set char)
                        :x (* 128 col) :y (* 128 row)
                        :xhandle 0 :yhandle 0))))))

(defn run [canvas tile-set]
  (go
    (c/with-sprite :tilemap
      [
       background-2 (js/PIXI.TilingSprite.
                     (t/sub-texture
                      (r/get-texture :background-2 :nearest)
                      [0 0] [1024 1024])
                     10000 10000)


       background (js/PIXI.TilingSprite.
                   (t/sub-texture
                    (r/get-texture :background-1 :nearest)
                    [0 0] [1024 1024])
                   10000 10000)

       banana-tree (s/make-sprite :tree
                                  :scale 1
                                  :yhandle 1)

       pyramid-base (s/make-container
                     :children (make-tiles tile-set titlescreen-map-3)
                     :xhandle 0.5
                     :yhandle 1
                     :scale 1
                     )
       ground (s/make-container
               :children (make-tiles tile-set titlescreen-map-1)
               :scale 1
               :xhandle 0.5
               )
       title (s/make-sprite :title
                            :scale 1
                            :xhandle 0.5
                            :yhandle 0.5
                            :y (* 64 -4)
                            )
       ]

      (async/go-while (not (controls/start-game?))
                (while true
                  (<! (keyboard-controls canvas
                                         "Overview"
                                         "Avoid the enemies" "Collect the runes"
                                         "Throw the runes" "Regrow the forest"
                                         "Draw down the heart" "Merge with Otoch"))
                  (<! (e/wait-frames 60))

                  (<! (keyboard-controls canvas
                                         "Keyboard Controls"
                                         "Cursor keys" "Move player"
                                         "Z or Spacebar" "Jump"
                                         "X" "Throw rune"))
                  (<! (e/wait-frames 60))

                  (<! (keyboard-controls canvas
                                         "Gamepad Controls"
                                         "Analogue stick" "Move player"
                                         "X or Y button" "Jump"
                                         "A or B button" "Throw rune"))
                  (<! (e/wait-frames 60))

                  (<! (keyboard-controls canvas
                                         "Alternate Keyboard Controls"
                                         "W A S D" "Move player"
                                         "Comma or Spacebar" "Jump"
                                         "Full Stop" "Throw rune"))
                  (<! (e/wait-frames 60))


                  (<! (keyboard-controls canvas
                                         "Added Post Game Jam"
                                         "Title screen" "Ladders"
                                         "More enemies" "Better level design"
                                         "Bug fixes" "More bug fixes"))
                  (<! (e/wait-frames 60))

                  (<! (keyboard-controls canvas
                                         "Credits"
                                         "Graphics and Sound" "Chris McCormick"
                                         "Code" "Crispin Wellington"
                                         "Music" "Pyotr Tchaikovsky"))
                  (<! (e/wait-frames 60))



                  )
                )

      (loop [n 0]
        (s/set-pos! background-2
                    (vec2/vec2 (+ (sin -10 freq n) -5000) -5000))
        (s/set-pos! background
                    (vec2/vec2 (- 5000) -5000))
        (s/set-pos! banana-tree
                    (vec2/vec2 (+ (sin 10 freq n) -32) (+ 5 (* 128 1))))
        (s/set-pos! pyramid-base
                    (vec2/vec2 (+ (sin 15 freq n) 128 32) (+ 5 (* 128 1))))
        (s/set-pos! ground
                    (vec2/vec2 (sin 20 freq n) (* 128 1)))

        (<! (e/next-frame))
        (when-not (controls/start-game?)
          (recur (inc n)))))))
