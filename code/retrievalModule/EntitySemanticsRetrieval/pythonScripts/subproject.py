import json
import os
import subprocess
subprojects={}

with open("bugList.txt", "r",encoding="utf-8") as f:
    lines = f.readlines()
    for line in lines:
        line = line.strip()
        parts = line.split("\t")
        PID = parts[1]
        if PID == "Farm" or PID == "Math":
            continue
        if PID == "Math_4j":
            PID = "Math"
        subproject = parts[3]
        bugids = []
        for ids in parts[5].split(",") :
            idss = ids.split("-")
            if len(idss) == 1:
                bugids.append(idss[0])
            else:
                if(len(idss) != 2):
                    print(len(idss))
                    print(PID)
                low = (int)(idss[0])
                high = (int)(idss[1])
                while low <= high:
                    bugids.append(str(low))
                    low += 1
        for bugid in bugids:
            #print(PID+"-"+bugid)
            # if PID+"-"+bugid != "AaltoXml-1":
            #     continue
            print("bugid:")
            print(bugid)
            if PID == "Imaging" and bugid == "16":
                continue
            if len(parts[3]) == 0:
                subprojects[PID+"-"+bugid] = "" 
            else:
                subprojects[PID+"-"+bugid] = parts[3] 
f = open("subprojects.json","w")
json.dump(subprojects,f)
f.close()
