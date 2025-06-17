import spacy
from openai import Client as OpenAIClient

from app.api_keys import openai_key, open_weather_map_key

nlp_german = spacy.load("de_core_news_sm")

nlp_multi = spacy.load("xx_ent_wiki_sm")

openai_client = OpenAIClient(
    api_key=openai_key
)