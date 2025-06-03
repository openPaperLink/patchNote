import json

res = {}
base_dir = ""
with open(base_dir+"bug_info_4j2.txt", "r", encoding="utf-8") as f:
    bug_lines = f.readlines()

# pattern of patches to generated
with open(base_dir+"patch_patterns_4j2.txt", "r", encoding="utf-8") as f:
    pattern_lines = f.readlines()

#pattern of patches in explanation dataset
with open(base_dir+"patch_patterns_4j1.txt", "r", encoding="utf-8") as f:
    pattern_lines_4j1 = f.readlines()
for line in bug_lines:
    line = line.strip()
    bugid = line.split(';')[0]

    own_pattern = []
    for pattern_line in pattern_lines:
        parts = pattern_line.strip().split(',')
        if parts[0] == bugid:
            own_pattern = parts[1:]
            break

    if not own_pattern:
        own_pattern = ['OtherStatement']

    intersection_dict = {}
    for idx, pattern_line in enumerate(pattern_lines_4j1):
        parts = pattern_line.strip().split(',')
        cur_bugid = parts[0]
        if cur_bugid == bugid:
            continue
        cur_pattern = parts[1:]
        intersection_count = len(set(own_pattern) & set(cur_pattern))

        if intersection_count == 0:
            continue

        if intersection_count not in intersection_dict:
            intersection_dict[intersection_count] = [cur_bugid]
        else:
            intersection_dict[intersection_count].append(cur_bugid)
    print(intersection_dict)
    if intersection_dict:
        max_key = max(intersection_dict.keys())
        res[bugid] = intersection_dict[max_key]

with open("./examples_4j2.json", "w", encoding="utf-8") as f:
    json.dump(res, f, ensure_ascii=False, indent=2)




