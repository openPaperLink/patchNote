import os
import subprocess
failedList = []
stdoutL = []
stderrL = []

def record_failed_cases(result,failedList,bugid,stdoutLL,stderrLL,ver):
    if result.returncode != 0:
        failedList.append(bugid+"-"+ver+"\n")
        #stdoutLL.append(bugid+"\n"+result.stdout)
        #stderrLL.append(bugid+"\n"+result.stderr)

with open("bugList.txt", "r",encoding="utf-8") as f:
    lines = f.readlines()
    for line in lines:
        line = line.strip()
        parts = line.split("\t")
        print(parts)
        PID = parts[1]
        
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
            #if PID == "Math_4j" and (int)(bugid) > 35:
            #    continue
            basedir = "trigger_tests/"+PID+"/"
            subprocess.run("mkdir -p "+basedir,shell=True)
            subprocess.run("cp growingBugRepository/framework/projects/"+PID+"/trigger_tests/"+bugid+" "+ basedir,shell=True)
            
