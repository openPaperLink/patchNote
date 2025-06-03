import os
import random
import argparse
from openai import OpenAI
import json

class Chat:
    def __init__(self, url,key,conversation_list=[]) -> None:
        self.conversation_list = []
        self.client = OpenAI(
            base_url=url,  # selected LLM URL
            api_key=key  # selected LLM key
        )
    def show_conversation(self, msg_list,bugid):
        for msg in msg_list[-2:]:
            if msg['role'] == 'user': 
                pass
            else: 
                p = bugid.split('-')[0]
                os.makedirs("./explanations", exist_ok=True)
                f = open("./explanations/"+bugid+".md","w",encoding="utf-8")
                message = msg['content']
                f.write(message)
                f.close()
                
    def ask(self,info,bugid,modelName):
        self.conversation_list.append({"role": "user", "content": info})
        completion = self.client.chat.completions.create(model=modelName, messages=self.conversation_list,temperature=0.0,top_p=1.0)
        answer = completion.choices[0].message.content
        self.conversation_list.append({"role": "assistant", "content": answer})
        self.show_conversation(self.conversation_list,bugid)

    def add_info(self,info):
        self.conversation_list.append({"role": "user", "content": info})
    def add_sys_info(self,info):
        self.conversation_list.append({"role": "system", "content": info})


def main(base_dir,modelName,bug_list_file,api_url, api_key):
    with open(base_dir+bug_list_file, 'r', encoding='utf-8') as file:
        lines = file.readlines()
        lines1 = lines.copy()
        for cn, line in enumerate(lines):
            talk = Chat()
            line = line.strip()
            bugid = line.split(';')[0]

            p = bugid.split('-')[0]
            id = bugid.split('-')[1]

            count_limit = 6
            order = ['patches/','classesInfo/','methodsInfo/','variablesInfo/','testsInfo/','examples/']
            all_info  = ""
            for i in range(count_limit):
                info = ""
                if i == 0:
                    info_file = order[i]+p+'/'+id+'.src.patch'
                elif i == 5:
                    dic = json.load(open("examples_gb.json", "r", encoding="utf-8"))
                    if bugid not in dic:
                        n = random.randint(0, 205)
                        random_line = lines1[n]
                        bugid1 = random_line.split(';')[0]
                        while (bugid1 == bugid):
                            n = random.randint(0, 205)
                            random_line = lines1[n]
                            bugid1 = random_line.split(';')[0]
                    else:
                        samples = dic[bugid]
                        index = random.randint(0, len(samples)-1)
                        bugid1 = samples[index]
                    p1 = bugid1.split('-')[0]
                    id1 = bugid1.split('-')[1]

                    example_patch = open("path2Patch" + p1 + '/' + id1 + '.src.patch', 'r',encoding="utf-8").read() # patch enclosing path
                    example_methods_info = open(f"methodsInfoPath/{bugid1}.json","r",encoding="utf-8").read() # path to methods info
                    example_variables_info = open(f"variablesInfoPath/{bugid1}.json","r",encoding="utf-8").read() #path to variable info
                    example_classes_info = open(f"classesInfoPath/{bugid1}.json", "r",encoding="utf-8").read() # path to classes info
                    example_tests_info = open(f"testsInfoPath/{bugid1}.json", "r",encoding="utf-8").read() # path to test info
                    example_explanation = open("exampleDatabasePath" + p1 + '/' + bugid1 + '.txt','r', encoding="utf-8").read() # path to examples
                    info += "===== Example (start)=====\n\n\tpatch of the example:\n" + example_patch + "\n\n"
                    info += "\tclasses info of example:\n" + example_classes_info + "\n\n"
                    info += "\tmethods info of example:\n" + example_methods_info + "\n\n"
                    info += "\tvariables info of example:\n" + example_variables_info + "\n\n"
                    info += "\ttests info of example:\n" + example_tests_info + "\n\n"
                    info += "\texplanation of example\n"+example_explanation+"\n\n===== Example (end) =====\n\n"

                else:
                    info_file = order[i] + bugid + ".json"

                if  i != 5 and os.path.exists(base_dir+info_file) :
                    file = open(base_dir + info_file, 'r',encoding="utf-8")
                    info = file.read()
                    file.close()

                if i == 0:
                    prompt = ("You are an experienced Java developer skilled at explaining patches. "
                              "The following patch fixes a bug, but I need help understanding it—"
                              "please provide an explanation in exactly one paragraph of four sentences: "
                              "(1) the cause of the bug, (2) its impact, (3) evidence supporting your analysis, and (4) how the bug was resolved, with no concluding sentence."
                              " I will provide: (1) the patch, (2) information on variables in the modified lines and the values assigned to them, "
                              "(3) details of the enclosing method and any invoked methods in the modified or assignment lines, "
                              "(4) the class where patch code is located  and the classes where these variables and methods are defined, and "
                              "(5) the triggering test cases. (6) an example including above all information and corresponding correct explanation. You can learn from the example and following the format and style. "
                              "I’m an experienced developer as well, so keep the explanation concise and focused and avoid unnecessary verbosity.")
                    talk.add_sys_info(prompt)
                all_info += info
            talk.ask(all_info,bugid,modelName,api_url, api_key)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Run ChatGPT explanation generation with specific parameters.")
    parser.add_argument("--modelName", required=True, type=str, help="Name of the LLM model to use, e.g., gpt-4o")
    parser.add_argument("--base_dir", required=True, type=str, help="Base directory where data files are stored")
    parser.add_argument("--bug_list_file", required=True, type=str, help="File name of the bug list, for exmaple: bug_info_4j1.txt")
    parser.add_argument("--api_url", required=True, type=str, help="Base URL for the OpenAI-compatible API")
    parser.add_argument("--api_key", required=True, type=str, help="API key for the OpenAI-compatible service")
    args = parser.parse_args()
    
    main(args.base_dir, args.modelName, args.bug_list_file,args.api_url, args.api_key)