from flask import Flask
from flask_session import Session
from app.extensions import db, jwt
from config import Config

from app.routes import auth_bp, api_bp, costs_bp, home_bp

def create_app():
    app = Flask(__name__)
    app.config.from_object(Config)  # Lädt Konfiguration aus der Datei `config.py`
    
    db.init_app(app)  # Initialisierung der Datenbank
    
    jwt.init_app(app)  # Initialisierung von JWT
    
    with app.app_context():
        db.create_all()
    
    
    # Registrierung der Blueprints
    app.register_blueprint(api_bp, url_prefix='/api')    # Routen mit Präfix `/api`
    app.register_blueprint(auth_bp, url_prefix='/auth')  # Routen mit Präfix `/user`
    app.register_blueprint(costs_bp, url_prefix='/costs')  # Routen mit Präfix `/costs`
    app.register_blueprint(home_bp, url_prefix='/')  # Routen mit Präfix `/`
    return app