int lastVal = 264;
int t = 0;

void setup() {
  Serial.begin(9600);
}

void loop() {
  int val = analogRead(0);
  Serial.print(t);
  Serial.write(",");
  Serial.print(val);
  Serial.write(",");
  Serial.print(val - lastVal);
  Serial.write("\n");
  delay(10);
  t += 10;
  lastVal = val;
}
