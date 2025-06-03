import json
import os
import subprocess
failedList = []
stdoutL = []
stderrL = []
classes_path = {}
tests_path = {}
def record_result(result,failedList,bugid,stdoutLL,stderrLL,ver):
    if result.returncode != 0:
        failedList.append(bugid+"-"+ver+"\n")
        
    else:
        
        f = open("tmp.txt","r")
        s = f.read()
        f.close()
        if ver == "tests":
            tests_path[bugid] = s.strip()
        else:
            classes_path[bugid] = s.strip()


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
            if PID == "Imaging" and bugid == "16":
                continue
            basedir = "path2SourceCode/"+PID+"/"+PID+"_"+bugid+"_buggy/" #source code path
            command1 = "defects4j export -p dir.src.classes -o tmp.txt -w " + basedir
            command2 = "defects4j export -p dir.src.tests -o tmp.txt -w " + basedir
            if len(parts[3]) == 0:
                result = subprocess.run(command1 , shell=True)
                record_result(result,failedList,PID+"-"+bugid,stdoutL,stderrL,"classes")
                result = subprocess.run(command2, shell=True)
                record_result(result,failedList,PID+"-"+bugid,stdoutL,stderrL,"tests")
            
            else:
                result = subprocess.run(command1+parts[3], shell=True)
                record_result(result,failedList,PID+"-"+bugid,stdoutL,stderrL,"classes")
                
                result = subprocess.run(command2+parts[3], shell=True)
                record_result(result,failedList,PID+"-"+bugid,stdoutL,stderrL,"tests")
            
f = open("classes_path.json","w")
json.dump(classes_path,f)
f.close()
f = open("tests_path.json","w")
json.dump(tests_path,f)
f.close()
f = open("failed_list.txt","w")
f.writelines(failedList)
f.close()
#f = open("stdout.txt","w")
#f.write(stdoutL)
#f.close()
#f = open("stderr.txt","w")
#f.write(stderrL)
