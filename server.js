import express from "express";
import fs from "node:fs/promises";
import path from "node:path";
import {fileURLToPath} from "node:url";

const __dirname=path.dirname(fileURLToPath(import.meta.url));
const app=express();
const PORT=process.env.PORT||3000;
const musicDir=path.join(__dirname,"music");
const publicDir=path.join(__dirname,"public");
const audioExt=/\.(mp3|wav|ogg|m4a|aac|flac)$/i;

await fs.mkdir(musicDir,{recursive:true});
app.use(express.static(publicDir));
app.use("/music",express.static(musicDir,{maxAge:"1h"}));

app.get("/api/music",async(_req,res)=>{
  try{
    const entries=await fs.readdir(musicDir,{withFileTypes:true});
    const tracks=entries.filter(e=>e.isFile()&&audioExt.test(e.name))
      .map(e=>({name:e.name,url:"/music/"+encodeURIComponent(e.name)}))
      .sort((a,b)=>a.name.localeCompare(b.name,undefined,{numeric:true,sensitivity:"base"}));
    res.json(tracks);
  }catch(err){res.status(500).json({error:"Could not read music folder"});}
});

app.use((_req,res)=>res.sendFile(path.join(publicDir,"index.html")));
app.listen(PORT,()=>console.log(`AeroPlayer listening on :${PORT}`));
