VTABLE(_Main) {
    <empty>
    Main
}

FUNCTION(_Main_New) {
memo ''
_Main_New:
    _T0 = 4
    parm _T0
    _T1 =  call _Alloc
    _T2 = VTBL <_Main>
    *(_T1 + 0) = _T2
    return _T1
}

FUNCTION(main) {
memo ''
main:
    _T4 = "first"
    _T5 = 3
    _T6 = 0
    _T7 = (_T5 < _T6)
    if (_T7 == 0) branch _L10
    _T8 = "Decaf runtime error: Cannot create negative-sized array\n"
    _T9 = "Decaf runtime error: The length of the created array should not be less than 0."
    parm _T9
    call _PrintString
    call _Halt
_L10:
    _T10 = 4
    _T11 = (_T10 * _T5)
    _T12 = (_T10 + _T11)
    parm _T12
    _T13 =  call _Alloc
    *(_T13 + 0) = _T5
    _T13 = (_T13 + _T12)
_L11:
    _T12 = (_T12 - _T10)
    if (_T12 == 0) branch _L12
    _T13 = (_T13 - _T10)
    *(_T13 + 0) = _T4
    branch _L11
_L12:
    _T3 = _T13
    _T14 = 1
    _T15 = *(_T3 - 4)
    _T16 = (_T14 < _T15)
    if (_T16 == 0) branch _L13
    _T17 = 0
    _T18 = (_T14 < _T17)
    if (_T18 == 0) branch _L14
_L13:
    _T19 = "Decaf runtime error: Array subscript out of bounds\n"
    parm _T19
    call _PrintString
    call _Halt
_L14:
    _T20 = 4
    _T21 = (_T14 * _T20)
    _T22 = (_T3 + _T21)
    _T23 = *(_T22 + 0)
    _T24 = "second"
    _T25 = 4
    _T26 = (_T14 * _T25)
    _T27 = (_T3 + _T26)
    *(_T27 + 0) = _T24
    _T28 = 0
    _T29 = *(_T3 - 4)
    _T30 = (_T28 < _T29)
    if (_T30 == 0) branch _L15
    _T31 = 0
    _T32 = (_T28 < _T31)
    if (_T32 == 0) branch _L16
_L15:
    _T33 = "Decaf runtime error: Array subscript out of bounds\n"
    parm _T33
    call _PrintString
    call _Halt
_L16:
    _T34 = 4
    _T35 = (_T28 * _T34)
    _T36 = (_T3 + _T35)
    _T37 = *(_T36 + 0)
    parm _T37
    call _PrintString
    _T38 = "\n"
    parm _T38
    call _PrintString
    _T39 = 1
    _T40 = *(_T3 - 4)
    _T41 = (_T39 < _T40)
    if (_T41 == 0) branch _L17
    _T42 = 0
    _T43 = (_T39 < _T42)
    if (_T43 == 0) branch _L18
_L17:
    _T44 = "Decaf runtime error: Array subscript out of bounds\n"
    parm _T44
    call _PrintString
    call _Halt
_L18:
    _T45 = 4
    _T46 = (_T39 * _T45)
    _T47 = (_T3 + _T46)
    _T48 = *(_T47 + 0)
    parm _T48
    call _PrintString
    _T49 = "\n"
    parm _T49
    call _PrintString
}

