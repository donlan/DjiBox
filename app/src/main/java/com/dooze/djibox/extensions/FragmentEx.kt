package com.dooze.djibox.extensions

import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * @author 梁桂栋
 * @date 2022/8/31  23:55.
 * e-mail 760625325@qq.com
 * GitHub: https://github.com/donlan
 * description: com.dooze.djibox.extensions
 * @version 1.0
 */


val BottomSheetDialogFragment.behavior
    get() = (dialog as? BottomSheetDialog)?.behavior
