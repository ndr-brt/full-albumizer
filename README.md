# Full Albumizer

To create a video with your songs and an image.

## Launch

Excecute
```
mvn compile exec:java -Dexec.mainClass="ndr.brt.FullAlbumizer" -Dexec.args="'<path_to_the_album_folder>'"
```

##TODO
* Handle the "width not divisible by 2 (597x600)" problem
* Eventually download cover image from the internet
* Handle progress