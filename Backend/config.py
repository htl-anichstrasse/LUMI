import os

class Config:
    # General
    # SESSION_PERMANENT = True
    # SESSION_TYPE = 'filesystem'
    # SESSION_USE_SIGNER = True
    # SESSION_PERMANENT = False
    # SECRET_KEY = os.getenv('SECRET_KEY', 'your_secret_key')
    # Database
    SQLALCHEMY_DATABASE_URI = os.getenv('DATABASE_URI', 'mysql+mysqlconnector://root:root@localhost/vector_db')
    SQLALCHEMY_TRACK_MODIFICATIONS = False
    # JWT
    JWT_SECRET_KEY = 'YOUR_JWT_SECRET_KEY'
    JWT_ACCESS_TOKEN_EXPIRES = 3600 # 1 hour
    JWT_REFRESH_TOKEN_EXPIRES= 15_552_000 # 6 months