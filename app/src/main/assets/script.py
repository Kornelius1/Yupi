import onnx

model = onnx.load("ecapa_tdnn.onnx")

for inp in model.graph.input:
    print("INPUT :", inp.name)
    print("SHAPE :", inp.type.tensor_type.shape)
    print("TYPE  :", inp.type.tensor_type.elem_type)

for out in model.graph.output:
    print("OUTPUT:", out.name)
    print("SHAPE :", out.type.tensor_type.shape)
    print("TYPE  :", out.type.tensor_type.elem_type)