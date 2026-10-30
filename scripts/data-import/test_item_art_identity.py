#!/usr/bin/env python3
"""Pure matching regressions: no network, no asset writes."""
import importlib.util,unittest
from pathlib import Path
p=Path(__file__).with_name('match-bulbagarden-items.py');spec=importlib.util.spec_from_file_location('item_art_match',p);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class ItemArtIdentityTest(unittest.TestCase):
 def test_full_item_name_wins_before_roman_game_tag(self):
  self.assertEqual(('bridgemailv',None,'full-item-name'),m.parse_filename('File:Bag BridgeMail V Sprite.png',{'bridgemailv'}))
  self.assertEqual(('potion','SV','game-tag-stripped'),m.parse_filename('File:Bag Potion SV Sprite.png',{'potion'}))
 def test_eight_explicit_slugs_are_not_duplicate_name_guesses(self):
  for base,english in [('ultra-ball','Ultra Ball'),('great-ball','Great Ball'),('poke-ball','Poké Ball'),('heavy-ball','Heavy Ball')]:
   items=[{'id':1,'slug':base,'name':english,'category':'balls','iconId':None},{'id':2,'slug':'la'+base,'name':english,'category':'balls','iconId':None}]
   files=[{'pageid':1,'title':f'File:Bag {english} SV Sprite.png'},{'pageid':2,'title':f'File:Bag {english} LA Sprite.png'}]
   rows,unmatched=m.match_records(items,files)
   self.assertEqual([],unmatched);self.assertEqual(2,len(rows))
   self.assertEqual('SV',rows[0]['source']['gameTag']);self.assertEqual('LA',rows[1]['source']['gameTag'])
 def test_unreviewed_duplicate_name_still_fails_closed(self):
  rows,excluded=m.match_records([{'id':1,'slug':'key-a','name':'Secret Key','category':'key','iconId':None},{'id':2,'slug':'key-b','name':'Secret Key','category':'key','iconId':None}],[{'pageid':1,'title':'File:Bag Secret Key Sprite.png'}])
  self.assertEqual([],rows);self.assertEqual(2,len(excluded))
if __name__=='__main__':unittest.main()
