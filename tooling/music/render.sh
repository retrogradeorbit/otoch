timidity -c fatboy.cfg nutcracker-arabian.mid -Or1Ssl -s44.1 -o- | sox -t raw -b 16 -e signed -r 44.1k -c 2 - ../resources/public/music/arabian.ogg
timidity -c fatboy.cfg nutcracker-flowers.mid -Or1Ssl -s44.1 -o- | sox -t raw -b 16 -e signed -r 44.1k -c 2 - ../resources/public/music/flowers.ogg
