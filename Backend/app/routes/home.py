from flask import Blueprint, render_template

home_bp = Blueprint('index', __name__)

@home_bp.route('/')
def home():
    return render_template('index.html')