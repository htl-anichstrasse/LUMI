# app/api/__init__.py

from flask import Blueprint

# Erstelle den Blueprint
api_bp = Blueprint('api', __name__)

# Importiere die Routen
from .vector import *
from .message import *
from .geo_data import *
from .query import *
from .mensa import *
from .generate_response import *
from .tts import *