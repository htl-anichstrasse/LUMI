from flask_sqlalchemy import SQLAlchemy
from flask_jwt_extended import JWTManager
import chromadb
from chromadb.utils import embedding_functions
from app.api_keys import openai_key

db = SQLAlchemy()
chroma_client = chromadb.PersistentClient("./chroma_db")

get_embeddings = embedding_functions.OpenAIEmbeddingFunction(
    api_key=openai_key,
    model_name="text-embedding-3-small"
)

user_collection = chroma_client.get_or_create_collection(
    name="user",
    embedding_function=get_embeddings
)

jwt = JWTManager()

blacklist = set()

@jwt.token_in_blocklist_loader
def check_if_token_in_blacklist(jwt_header, jwt_payload):
    return jwt_payload["jti"] in blacklist