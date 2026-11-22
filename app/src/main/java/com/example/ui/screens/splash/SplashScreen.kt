package com.example.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.CharcoalDark
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PrimaryYellowDark
import com.example.ui.theme.PureWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
  onSplashComplete: () -> Unit,
) {
  val progress = remember { Animatable(0f) }
  var revealStarted by remember { mutableStateOf(false) }

  val logoScale by animateFloatAsState(
    targetValue = if (revealStarted) 1f else 0.88f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessLow,
    ),
    label = "logoScale",
  )
  val contentAlpha by animateFloatAsState(
    targetValue = if (revealStarted) 1f else 0f,
    animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
    label = "contentAlpha",
  )
  val cartOffsetRatio by animateFloatAsState(
    targetValue = if (revealStarted) 0f else 48f,
    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
    label = "cartOffset",
  )

  LaunchedEffect(Unit) {
    revealStarted = true
    launch {
      progress.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1750, easing = FastOutSlowInEasing),
      )
    }
    delay(1850L)
    onSplashComplete()
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(PureWhite)
      .testTag("splash_screen_root"),
  ) {
    val isCompactHeight = maxHeight < 640.dp
    val logoSize = if (isCompactHeight) 116.dp else 148.dp
    val cartHeight = if (isCompactHeight) 150.dp else 210.dp

    // 1. الخلفية الفنية المرسومة بالكود: هالة ناعمة، موجة علوية صفراء، أفق مدينة خفيف، وموجات سفلية متداخلة
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height

      // هالة دافئة ناعمة خلف الشعار
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            Color(0xFFFFFCEB),
            Color(0xFFFFFDF6),
            PureWhite,
          ),
          center = Offset(w * 0.5f, h * 0.34f),
          radius = w * 0.72f,
        ),
        radius = w * 0.72f,
        center = Offset(w * 0.5f, h * 0.34f),
      )

      // موجة علوية صفراء ناعمة (تدرج ذهبي دافئ)
      val topShadowWave = Path().apply {
        moveTo(0f, 0f)
        lineTo(w * 0.60f, 0f)
        cubicTo(
          w * 0.45f, h * 0.08f,
          w * 0.16f, h * 0.14f,
          0f, h * 0.24f,
        )
        close()
      }
      drawPath(
        path = topShadowWave,
        color = PrimaryYellow.copy(alpha = 0.28f),
      )

      val topMainWave = Path().apply {
        moveTo(0f, 0f)
        lineTo(w * 0.54f, 0f)
        cubicTo(
          w * 0.40f, h * 0.075f,
          w * 0.15f, h * 0.125f,
          0f, h * 0.215f,
        )
        close()
      }
      drawPath(
        path = topMainWave,
        brush = Brush.linearGradient(
          colors = listOf(Color(0xFFFFDF38), PrimaryYellow, Color(0xFFF2AD00)),
          start = Offset(0f, 0f),
          end = Offset(w * 0.5f, h * 0.22f),
        ),
      )

      // خط أفق المدينة التجاري الخفيف في المنتصف السفلي
      val skylineBaseY = h * 0.78f
      val skylineColor = CharcoalBlack.copy(alpha = 0.055f)
      val buildings = listOf(
        0.02f to 0.09f,
        0.09f to 0.14f,
        0.16f to 0.08f,
        0.23f to 0.16f,
        0.32f to 0.11f,
        0.41f to 0.13f,
        0.52f to 0.10f,
        0.60f to 0.17f,
        0.71f to 0.12f,
        0.80f to 0.15f,
        0.89f to 0.09f,
      )
      buildings.forEachIndexed { index, (xRatio, heightRatio) ->
        val bx = w * xRatio
        val bw = w * (if (index % 2 == 0) 0.065f else 0.082f)
        val bh = h * heightRatio
        drawRect(
          color = skylineColor,
          topLeft = Offset(bx, skylineBaseY - bh),
          size = androidx.compose.ui.geometry.Size(bw, bh),
        )
      }

      // الموجة السفلية الذهبية الخلفية
      val bottomYellowWave = Path().apply {
        moveTo(0f, h * 0.735f)
        cubicTo(
          w * 0.28f, h * 0.845f,
          w * 0.70f, h * 0.805f,
          w, h * 0.695f,
        )
        lineTo(w, h)
        lineTo(0f, h)
        close()
      }
      drawPath(
        path = bottomYellowWave,
        brush = Brush.horizontalGradient(
          colors = listOf(Color(0xFFFFE140), PrimaryYellow, Color(0xFFF0AD00)),
        ),
      )

      // لمعة صفراء فاتحة إضافية فوق موجة الأسود الفحمي
      val bottomYellowHighlight = Path().apply {
        moveTo(0f, h * 0.78f)
        cubicTo(
          w * 0.30f, h * 0.86f,
          w * 0.72f, h * 0.82f,
          w, h * 0.715f,
        )
        lineTo(w, h)
        lineTo(0f, h)
        close()
      }
      drawPath(
        path = bottomYellowHighlight,
        color = Color(0xFFFFEB70).copy(alpha = 0.45f),
      )

      // الموجة السفلية بالأسود الفحمي الفاخر
      val bottomCharcoalWave = Path().apply {
        moveTo(0f, h * 0.845f)
        cubicTo(
          w * 0.26f, h * 0.79f,
          w * 0.68f, h * 0.875f,
          w, h * 0.735f,
        )
        lineTo(w, h)
        lineTo(0f, h)
        close()
      }
      drawPath(
        path = bottomCharcoalWave,
        brush = Brush.verticalGradient(
          colors = listOf(CharcoalBlack, CharcoalDark),
          startY = h * 0.73f,
          endY = h,
        ),
      )
    }

    // 2. أيقونات تجارية خفيفة عائمة في الخلفية (تطابق الروح التصميمية للصورة)
    Box(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)
        .alpha(contentAlpha),
    ) {
      Icon(
        imageVector = Icons.Outlined.ShoppingCart,
        contentDescription = null,
        tint = CharcoalBlack.copy(alpha = 0.07f),
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(start = 74.dp, top = 54.dp)
          .size(64.dp)
          .rotate(-10f),
      )
      Icon(
        imageVector = Icons.Outlined.LocalOffer,
        contentDescription = null,
        tint = CharcoalBlack.copy(alpha = 0.07f),
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(end = 68.dp, top = 46.dp)
          .size(54.dp)
          .rotate(28f),
      )
      Icon(
        imageVector = Icons.Outlined.CardGiftcard,
        contentDescription = null,
        tint = CharcoalBlack.copy(alpha = 0.07f),
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(end = 22.dp, top = 112.dp)
          .size(50.dp)
          .rotate(-8f),
      )
      Icon(
        imageVector = Icons.Outlined.CreditCard,
        contentDescription = null,
        tint = CharcoalBlack.copy(alpha = 0.07f),
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(end = 26.dp, top = 186.dp)
          .size(48.dp)
          .rotate(12f),
      )
    }

    // 3. المحتوى المركزي: الشعار الرسمي + الشعار النصي + القوس الذهبي + عربة التسوق
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)
        .padding(top = if (isCompactHeight) 42.dp else 72.dp, bottom = 128.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      // الشعار الرسمي للتطبيق (داخل إطار ناعم بظل هادئ وإشراقة ذهبية)
      Surface(
        modifier = Modifier
          .size(logoSize)
          .scale(logoScale),
        shape = RoundedCornerShape(32.dp),
        color = PureWhite,
        shadowElevation = 14.dp,
        border = BorderStroke(2.dp, PrimaryYellow.copy(alpha = 0.55f)),
      ) {
        Image(
          painter = painterResource(id = R.drawable.app_logo),
          contentDescription = stringResource(id = R.string.app_name),
          modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(30.dp)),
          contentScale = ContentScale.Crop,
        )
      }

      Spacer(modifier = Modifier.height(if (isCompactHeight) 14.dp else 22.dp))

      // الشعار النصي المعتمد: تسوق بذكاء .. عيش أفضل
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .alpha(contentAlpha)
          .padding(horizontal = 24.dp),
      ) {
        Text(
          text = stringResource(id = R.string.brand_tagline),
          style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = if (isCompactHeight) 20.sp else 23.sp,
          ),
          color = CharcoalBlack,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        // قوس ذهبي ناعم أسفل الشعار النصي
        Canvas(
          modifier = Modifier
            .width(190.dp)
            .height(10.dp),
        ) {
          val swooshPath = Path().apply {
            moveTo(0f, size.height * 0.75f)
            quadraticTo(
              size.width * 0.5f,
              -size.height * 0.35f,
              size.width,
              size.height * 0.65f,
            )
          }
          drawPath(
            path = swooshPath,
            brush = Brush.horizontalGradient(
              colors = listOf(
                PrimaryYellow.copy(alpha = 0.25f),
                PrimaryYellow,
                PrimaryYellowDark,
                PrimaryYellow.copy(alpha = 0.25f),
              ),
            ),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
          )
        }
      }

      Spacer(modifier = Modifier.height(if (isCompactHeight) 10.dp else 18.dp))

      // عربة التسوق ثلاثية الأبعاد بالحقيبتين الصفراء والسوداء
      Image(
        painter = painterResource(id = R.drawable.img_splash_cart),
        contentDescription = null,
        modifier = Modifier
          .height(cartHeight)
          .fillMaxWidth(0.78f)
          .offset(x = cartOffsetRatio.dp)
          .alpha(contentAlpha),
        contentScale = ContentScale.Fit,
      )
    }

    // 4. شريط التحميل السفلي داخل موجة الأسود الفحمي مع احترام منطقة التنقل
    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(bottom = 26.dp, start = 24.dp, end = 24.dp)
        .alpha(contentAlpha),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = "جاري التحميل...",
        style = MaterialTheme.typography.bodyMedium.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
        ),
        color = PureWhite,
      )

      Spacer(modifier = Modifier.height(10.dp))

      Box(
        modifier = Modifier
          .width(250.dp)
          .height(12.dp)
          .clip(RoundedCornerShape(50))
          .background(Color(0xFF1A1C22))
          .border(1.dp, Color(0xFF3E424E), RoundedCornerShape(50))
          .testTag("splash_loading_bar"),
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth(progress.value.coerceIn(0.04f, 1f))
            .fillMaxHeight()
            .clip(RoundedCornerShape(50))
            .background(
              Brush.horizontalGradient(
                colors = listOf(
                  Color(0xFFFFE75A),
                  PrimaryYellow,
                  PrimaryYellowDark,
                ),
              ),
            ),
        )
      }
    }
  }
}
