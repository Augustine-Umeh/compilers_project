* C-Minus Compilation to TM Code
* Standard prelude:
  0:     LD  6,  0(0)	load gp with maxaddress
  1:    LDA  5,  0(6)	copy gp to fp
  2:     ST  0,  0(0)	clear location 0
* Jump around i/o routines here
  3:    LDA  7,  7(7)	jump around i/o code
* code for input routine
  4:     ST  0, -1(5)	store return
  5:     IN  0,0,0	input
  6:     LD  7, -1(5)	return to caller
* code for output routine
  7:     ST  0, -1(5)	store return
  8:     LD  0, -2(5)	load output value
  9:    OUT  0,0,0	output
 10:     LD  7, -1(5)	return to caller
* End of standard prelude.
* allocating global var: x
* <- vardecl
* processing function: minloc
* jump around function body here
 11:    LDA  7, 88(7)	jump around fn body
 12:     ST  0, -1(5)	store return
* -> compound statement
* processing local var: i
* processing local var: x
* processing local var: k
* -> op
* -> id
* looking up id: k
 13:    LDA  0, -7(5)	load id address
* <- id
 14:     ST  0, -9(5)	op: push left
* -> id
* looking up id: low
 15:     LD  0, -3(5)	load id value
* <- id
 16:     LD  1, -9(5)	op: load left
 17:     ST  0,  0(1)	assign: store value
* <- op
* -> op
* -> id
* looking up id: x
 18:    LDA  0, -6(5)	load id address
* <- id
 19:     ST  0, -9(5)	op: push left
* -> subs
* -> id
* looking up id: low
 20:     LD  0, -3(5)	load id value
* <- id
 21:     ST  0,-10(5)	store array index
 22:    JLT  0,  1(7)	halt if subscript < 0
 23:    LDA  7,  1(7)	absolute jump if not
 24:   HALT  0,0,0	halt if subscript < 0
 25:     LD  0,-10(5)	reload index
 26:     LD  1, -2(5)	load array base addr
 27:    SUB  0,1,0	base is at top of array
 28:     LD  0,  0(0)	load value at array index
* <- subs
 29:     LD  1, -9(5)	op: load left
 30:     ST  0,  0(1)	assign: store value
* <- op
* -> op
* -> id
* looking up id: i
 31:    LDA  0, -5(5)	load id address
* <- id
 32:     ST  0, -9(5)	op: push left
* -> op
* -> id
* looking up id: low
 33:     LD  0, -3(5)	load id value
* <- id
 34:     ST  0,-10(5)	op: push left
* -> constant
 35:    LDC  0,  1(0)	load const
* <- constant
 36:     LD  1,-10(5)	op: load left
 37:    ADD  0,1,0	op +
* <- op
 38:     LD  1, -9(5)	op: load left
 39:     ST  0,  0(1)	assign: store value
* <- op
* -> while
* -> op
* -> id
* looking up id: i
 40:     LD  0, -5(5)	load id value
* <- id
 41:     ST  0, -9(5)	op: push left
* -> id
* looking up id: high
 42:     LD  0, -4(5)	load id value
* <- id
 43:     LD  1, -9(5)	op: load left
 44:    SUB  0,1,0	op <
 45:    JLT  0,  2(7)	br if true
 46:    LDC  0,  0(0)	false case
 47:    LDA  7,  1(7)	unconditional jmp
 48:    LDC  0,  1(0)	true case
* <- op
 49:    JEQ  0, 47(7)	while: jmp to end
* -> compound statement
* -> if
* -> op
* -> subs
* -> id
* looking up id: i
 50:     LD  0, -5(5)	load id value
* <- id
 51:     ST  0, -9(5)	store array index
 52:    JLT  0,  1(7)	halt if subscript < 0
 53:    LDA  7,  1(7)	absolute jump if not
 54:   HALT  0,0,0	halt if subscript < 0
 55:     LD  0, -9(5)	reload index
 56:     LD  1, -2(5)	load array base addr
 57:    SUB  0,1,0	base is at top of array
 58:     LD  0,  0(0)	load value at array index
* <- subs
 59:     ST  0, -9(5)	op: push left
* -> id
* looking up id: x
 60:     LD  0, -6(5)	load id value
* <- id
 61:     LD  1, -9(5)	op: load left
 62:    SUB  0,1,0	op <
 63:    JLT  0,  2(7)	br if true
 64:    LDC  0,  0(0)	false case
 65:    LDA  7,  1(7)	unconditional jmp
 66:    LDC  0,  1(0)	true case
* <- op
 67:    JEQ  0, 19(7)	if: jmp to else
* -> compound statement
* -> op
* -> id
* looking up id: x
 68:    LDA  0, -6(5)	load id address
* <- id
 69:     ST  0, -9(5)	op: push left
* -> subs
* -> id
* looking up id: i
 70:     LD  0, -5(5)	load id value
* <- id
 71:     ST  0,-10(5)	store array index
 72:    JLT  0,  1(7)	halt if subscript < 0
 73:    LDA  7,  1(7)	absolute jump if not
 74:   HALT  0,0,0	halt if subscript < 0
 75:     LD  0,-10(5)	reload index
 76:     LD  1, -2(5)	load array base addr
 77:    SUB  0,1,0	base is at top of array
 78:     LD  0,  0(0)	load value at array index
* <- subs
 79:     LD  1, -9(5)	op: load left
 80:     ST  0,  0(1)	assign: store value
* <- op
* -> op
* -> id
* looking up id: k
 81:    LDA  0, -7(5)	load id address
* <- id
 82:     ST  0, -9(5)	op: push left
* -> id
* looking up id: i
 83:     LD  0, -5(5)	load id value
* <- id
 84:     LD  1, -9(5)	op: load left
 85:     ST  0,  0(1)	assign: store value
* <- op
* <- compound statement
 86:    LDA  7,  0(7)	jmp to end
* <- if
* -> op
* -> id
* looking up id: i
 87:    LDA  0, -5(5)	load id address
* <- id
 88:     ST  0, -9(5)	op: push left
* -> op
* -> id
* looking up id: i
 89:     LD  0, -5(5)	load id value
* <- id
 90:     ST  0,-10(5)	op: push left
* -> constant
 91:    LDC  0,  1(0)	load const
* <- constant
 92:     LD  1,-10(5)	op: load left
 93:    ADD  0,1,0	op +
* <- op
 94:     LD  1, -9(5)	op: load left
 95:     ST  0,  0(1)	assign: store value
* <- op
* <- compound statement
 96:    LDA  7,-57(7)	while: absolute jmp to test
* <- while
* -> return
* -> id
* looking up id: k
 97:     LD  0, -7(5)	load id value
* <- id
 98:     LD  7, -1(5)	return to caller
* <- return
* <- compound statement
 99:     LD  7, -1(5)	return to caller
* <- fundecl
* processing function: sort
* jump around function body here
100:    LDA  7, 91(7)	jump around fn body
101:     ST  0, -1(5)	store return
* -> compound statement
* processing local var: i
* processing local var: k
* -> op
* -> id
* looking up id: i
102:    LDA  0, -5(5)	load id address
* <- id
103:     ST  0, -9(5)	op: push left
* -> id
* looking up id: low
104:     LD  0, -3(5)	load id value
* <- id
105:     LD  1, -9(5)	op: load left
106:     ST  0,  0(1)	assign: store value
* <- op
* -> while
* -> op
* -> id
* looking up id: i
107:     LD  0, -5(5)	load id value
* <- id
108:     ST  0, -9(5)	op: push left
* -> op
* -> id
* looking up id: high
109:     LD  0, -4(5)	load id value
* <- id
110:     ST  0,-10(5)	op: push left
* -> constant
111:    LDC  0,  1(0)	load const
* <- constant
112:     LD  1,-10(5)	op: load left
113:    SUB  0,1,0	op -
* <- op
114:     LD  1, -9(5)	op: load left
115:    SUB  0,1,0	op <
116:    JLT  0,  2(7)	br if true
117:    LDC  0,  0(0)	false case
118:    LDA  7,  1(7)	unconditional jmp
119:    LDC  0,  1(0)	true case
* <- op
120:    JEQ  0, 70(7)	while: jmp to end
* -> compound statement
* processing local var: t
* -> op
* -> id
* looking up id: k
121:    LDA  0, -6(5)	load id address
* <- id
122:     ST  0, -9(5)	op: push left
* -> call of function: minloc
123:     LD  0, -2(5)	load id value
124:     ST  0,-12(5)	store arg val
* -> id
* looking up id: i
125:     LD  0, -5(5)	load id value
* <- id
126:     ST  0,-13(5)	store arg val
* -> id
* looking up id: high
127:     LD  0, -4(5)	load id value
* <- id
128:     ST  0,-14(5)	store arg val
129:     ST  5,-10(5)	push ofp
130:    LDA  5,-10(5)	push frame
131:    LDA  0,  1(7)	load ac with ret ptr
132:    LDA  7,-121(7)	jump to fun loc
133:     LD  5,  0(5)	pop frame
* <- call
134:     LD  1, -9(5)	op: load left
135:     ST  0,  0(1)	assign: store value
* <- op
* -> op
* -> id
* looking up id: t
136:    LDA  0, -7(5)	load id address
* <- id
137:     ST  0, -9(5)	op: push left
* -> subs
* -> id
* looking up id: k
138:     LD  0, -6(5)	load id value
* <- id
139:     ST  0,-10(5)	store array index
140:    JLT  0,  1(7)	halt if subscript < 0
141:    LDA  7,  1(7)	absolute jump if not
142:   HALT  0,0,0	halt if subscript < 0
143:     LD  0,-10(5)	reload index
144:     LD  1, -2(5)	load array base addr
145:    SUB  0,1,0	base is at top of array
146:     LD  0,  0(0)	load value at array index
* <- subs
147:     LD  1, -9(5)	op: load left
148:     ST  0,  0(1)	assign: store value
* <- op
* -> op
* -> subs
* -> id
* looking up id: k
149:     LD  0, -6(5)	load id value
* <- id
150:     ST  0, -9(5)	store array index
151:    JLT  0,  1(7)	halt if subscript < 0
152:    LDA  7,  1(7)	absolute jump if not
153:   HALT  0,0,0	halt if subscript < 0
154:     LD  0, -9(5)	reload index
155:     LD  1, -2(5)	load array base addr
156:    SUB  0,1,0	base is at top of array
* <- subs
157:     ST  0, -9(5)	op: push left
* -> subs
* -> id
* looking up id: i
158:     LD  0, -5(5)	load id value
* <- id
159:     ST  0,-10(5)	store array index
160:    JLT  0,  1(7)	halt if subscript < 0
161:    LDA  7,  1(7)	absolute jump if not
162:   HALT  0,0,0	halt if subscript < 0
163:     LD  0,-10(5)	reload index
164:     LD  1, -2(5)	load array base addr
165:    SUB  0,1,0	base is at top of array
166:     LD  0,  0(0)	load value at array index
* <- subs
167:     LD  1, -9(5)	op: load left
168:     ST  0,  0(1)	assign: store value
* <- op
* -> op
* -> subs
* -> id
* looking up id: i
169:     LD  0, -5(5)	load id value
* <- id
170:     ST  0, -9(5)	store array index
171:    JLT  0,  1(7)	halt if subscript < 0
172:    LDA  7,  1(7)	absolute jump if not
173:   HALT  0,0,0	halt if subscript < 0
174:     LD  0, -9(5)	reload index
175:     LD  1, -2(5)	load array base addr
176:    SUB  0,1,0	base is at top of array
* <- subs
177:     ST  0, -9(5)	op: push left
* -> id
* looking up id: t
178:     LD  0, -7(5)	load id value
* <- id
179:     LD  1, -9(5)	op: load left
180:     ST  0,  0(1)	assign: store value
* <- op
* -> op
* -> id
* looking up id: i
181:    LDA  0, -5(5)	load id address
* <- id
182:     ST  0, -9(5)	op: push left
* -> op
* -> id
* looking up id: i
183:     LD  0, -5(5)	load id value
* <- id
184:     ST  0,-10(5)	op: push left
* -> constant
185:    LDC  0,  1(0)	load const
* <- constant
186:     LD  1,-10(5)	op: load left
187:    ADD  0,1,0	op +
* <- op
188:     LD  1, -9(5)	op: load left
189:     ST  0,  0(1)	assign: store value
* <- op
* <- compound statement
190:    LDA  7,-84(7)	while: absolute jmp to test
* <- while
* <- compound statement
191:     LD  7, -1(5)	return to caller
* <- fundecl
* processing function: main
* jump around function body here
192:    LDA  7,106(7)	jump around fn body
193:     ST  0, -1(5)	store return
* -> compound statement
* processing local var: i
* -> op
* -> id
* looking up id: i
194:    LDA  0, -2(5)	load id address
* <- id
195:     ST  0, -4(5)	op: push left
* -> constant
196:    LDC  0,  0(0)	load const
* <- constant
197:     LD  1, -4(5)	op: load left
198:     ST  0,  0(1)	assign: store value
* <- op
* -> while
* -> op
* -> id
* looking up id: i
199:     LD  0, -2(5)	load id value
* <- id
200:     ST  0, -4(5)	op: push left
* -> constant
201:    LDC  0, 10(0)	load const
* <- constant
202:     LD  1, -4(5)	op: load left
203:    SUB  0,1,0	op <
204:    JLT  0,  2(7)	br if true
205:    LDC  0,  0(0)	false case
206:    LDA  7,  1(7)	unconditional jmp
207:    LDC  0,  1(0)	true case
* <- op
208:    JEQ  0, 32(7)	while: jmp to end
* -> compound statement
* -> op
* -> subs
* -> id
* looking up id: i
209:     LD  0, -2(5)	load id value
* <- id
210:     ST  0, -4(5)	store array index
211:    JLT  0,  1(7)	halt if subscript < 0
212:    LDA  7,  1(7)	absolute jump if not
213:   HALT  0,0,0	halt if subscript < 0
214:     LD  0, -4(5)	reload index
215:    LDC  1, 10(0)	load array size
216:    SUB  0,0,1	index - size
217:    JGE  0,  1(7)	halt if subscript >= size
218:    LDA  7,  1(7)	skip halt if in range
219:   HALT  0,0,0	halt if subscript >= size
220:     LD  0, -4(5)	reload index
221:    LDA  1,  0(6)	load array base addr
222:    SUB  0,1,0	base is at top of array
* <- subs
223:     ST  0, -4(5)	op: push left
* -> call of function: input
224:     ST  5, -5(5)	push ofp
225:    LDA  5, -5(5)	push frame
226:    LDA  0,  1(7)	load ac with ret ptr
227:    LDA  7,-224(7)	jump to fun loc
228:     LD  5,  0(5)	pop frame
* <- call
229:     LD  1, -4(5)	op: load left
230:     ST  0,  0(1)	assign: store value
* <- op
* -> op
* -> id
* looking up id: i
231:    LDA  0, -2(5)	load id address
* <- id
232:     ST  0, -4(5)	op: push left
* -> op
* -> id
* looking up id: i
233:     LD  0, -2(5)	load id value
* <- id
234:     ST  0, -5(5)	op: push left
* -> constant
235:    LDC  0,  1(0)	load const
* <- constant
236:     LD  1, -5(5)	op: load left
237:    ADD  0,1,0	op +
* <- op
238:     LD  1, -4(5)	op: load left
239:     ST  0,  0(1)	assign: store value
* <- op
* <- compound statement
240:    LDA  7,-42(7)	while: absolute jmp to test
* <- while
* -> call of function: sort
241:    LDA  0,  0(6)	load id address
242:     ST  0, -6(5)	store arg val
* -> constant
243:    LDC  0,  0(0)	load const
* <- constant
244:     ST  0, -7(5)	store arg val
* -> constant
245:    LDC  0, 10(0)	load const
* <- constant
246:     ST  0, -8(5)	store arg val
247:     ST  5, -4(5)	push ofp
248:    LDA  5, -4(5)	push frame
249:    LDA  0,  1(7)	load ac with ret ptr
250:    LDA  7,-150(7)	jump to fun loc
251:     LD  5,  0(5)	pop frame
* <- call
* -> op
* -> id
* looking up id: i
252:    LDA  0, -2(5)	load id address
* <- id
253:     ST  0, -4(5)	op: push left
* -> constant
254:    LDC  0,  0(0)	load const
* <- constant
255:     LD  1, -4(5)	op: load left
256:     ST  0,  0(1)	assign: store value
* <- op
* -> while
* -> op
* -> id
* looking up id: i
257:     LD  0, -2(5)	load id value
* <- id
258:     ST  0, -4(5)	op: push left
* -> constant
259:    LDC  0, 10(0)	load const
* <- constant
260:     LD  1, -4(5)	op: load left
261:    SUB  0,1,0	op <
262:    JLT  0,  2(7)	br if true
263:    LDC  0,  0(0)	false case
264:    LDA  7,  1(7)	unconditional jmp
265:    LDC  0,  1(0)	true case
* <- op
266:    JEQ  0, 31(7)	while: jmp to end
* -> compound statement
* -> call of function: output
* -> subs
* -> id
* looking up id: i
267:     LD  0, -2(5)	load id value
* <- id
268:     ST  0, -7(5)	store array index
269:    JLT  0,  1(7)	halt if subscript < 0
270:    LDA  7,  1(7)	absolute jump if not
271:   HALT  0,0,0	halt if subscript < 0
272:     LD  0, -7(5)	reload index
273:    LDC  1, 10(0)	load array size
274:    SUB  0,0,1	index - size
275:    JGE  0,  1(7)	halt if subscript >= size
276:    LDA  7,  1(7)	skip halt if in range
277:   HALT  0,0,0	halt if subscript >= size
278:     LD  0, -7(5)	reload index
279:    LDA  1,  0(6)	load array base addr
280:    SUB  0,1,0	base is at top of array
281:     LD  0,  0(0)	load value at array index
* <- subs
282:     ST  0, -6(5)	store arg val
283:     ST  5, -4(5)	push ofp
284:    LDA  5, -4(5)	push frame
285:    LDA  0,  1(7)	load ac with ret ptr
286:    LDA  7,-280(7)	jump to fun loc
287:     LD  5,  0(5)	pop frame
* <- call
* -> op
* -> id
* looking up id: i
288:    LDA  0, -2(5)	load id address
* <- id
289:     ST  0, -4(5)	op: push left
* -> op
* -> id
* looking up id: i
290:     LD  0, -2(5)	load id value
* <- id
291:     ST  0, -5(5)	op: push left
* -> constant
292:    LDC  0,  1(0)	load const
* <- constant
293:     LD  1, -5(5)	op: load left
294:    ADD  0,1,0	op +
* <- op
295:     LD  1, -4(5)	op: load left
296:     ST  0,  0(1)	assign: store value
* <- op
* <- compound statement
297:    LDA  7,-41(7)	while: absolute jmp to test
* <- while
* <- compound statement
298:     LD  7, -1(5)	return to caller
* <- fundecl
299:     ST  5,-10(5)	push ofp
300:    LDA  5,-10(5)	push frame
301:    LDA  0,  1(7)	load ac with ret ptr
302:    LDA  7,-110(7)	jump to main loc
303:     LD  5,  0(5)	pop frame
* End of execution.
304:   HALT  0,0,0
