# app/auth/__init__.py

from flask import Blueprint

# Erstelle den Blueprint
auth_bp = Blueprint('auth', __name__)

from .login import *
from .register import *
from .logout import *
from .get_user import *
from .refresh import *