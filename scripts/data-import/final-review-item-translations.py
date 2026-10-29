#!/usr/bin/env python3
import json,re
from pathlib import Path
P=Path(__file__).parent
source=(P/'items-final-review-source.txt').read_text().splitlines();pairs=(P/'items-final-review-pairs.txt').read_text().splitlines()
assert len(source)==len(pairs)==94,(len(source),len(pairs))
out=json.loads((P/'reviewed-items.json').read_text())
nums=lambda s:re.findall(r'\d+(?:[.,/]\d+)*(?:%|×)?',s)
for original,row in zip(source,pairs):
 pt,es=row.split('|||');assert nums(original)==nums(pt)==nums(es),(original,nums(original),nums(pt),nums(es))
 out[original]={'pt':pt,'es':es}
(P/'reviewed-items.json').write_text(json.dumps(out,ensure_ascii=False,indent=2)+'\n')
assert len(out)==517,len(out)
print('All517uniqueitem summaries have reviewedtext/templates; numericorderpreserved.')
