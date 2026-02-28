class Token {

  public String m_value;
  public int m_line;
  public int m_column;
  
  Token (String value, int line, int column) {
    m_value = value;
    m_line = line;
    m_column = column;
  }

  public String toString() {
      return m_value;
  }
}