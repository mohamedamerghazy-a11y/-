package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import com.example.ui.theme.AlEmanDimens
import com.example.ui.theme.BorderNeutral
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.CharcoalDark
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.PrimaryYellowDark
import com.example.ui.theme.PrimaryYellowSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class BubbleSpec(
  val xFraction: Float,
  val baseRadiusDp: Float,
  val speedMultiplier: Float,
  val phaseOffset: Float,
  val swayAmplitudeDp: Float,
  val tint: Color,
)

private val CleaningBubbleSpecs = listOf(
  BubbleSpec(0.14f, 14f, 1.0f, 0.05f, 8f, Color(0xFF38BDF8)),
  BubbleSpec(0.28f, 9f, 1.3f, 0.35f, 6f, Color(0xFF0EA5E9)),
  BubbleSpec(0.45f, 12f, 0.9f, 0.62f, 9f, Color(0xFF38BDF8)),
  BubbleSpec(0.64f, 16f, 1.15f, 0.22f, 7f, Color(0xFFFFD000)),
  BubbleSpec(0.79f, 10f, 1.35f, 0.78f, 6f, Color(0xFF0EA5E9)),
  BubbleSpec(0.89f, 13f, 1.05f, 0.48f, 8f, Color(0xFF38BDF8)),
)

private val AquaFresh = Color(0xFF0284C7)
private val AquaFreshSoft = Color(0xFFE0F2FE)

/**
 * تصميم متحرك مخصص لحالة عربة التسوق الفارغة:
 * يستلهم روح شاشة البداية (الموجة الذهبية والفحمية والقوس الذهبي) مع فقاعات صابون صاعدة وبريق نظافة
 * يعبّر عن تخصص المتجر في المنظفات ومستلزمات النظافة المنزلية.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmptyCartCleaningHero(
  onShopNow: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "empty_cart_cleaning_motion")
  val bubbleProgress by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 5200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "bubble_rise",
  )
  val floatOffset by infiniteTransition.animateFloat(
    initialValue = -7f,
    targetValue = 7f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "emblem_float",
  )
  val sparklePulse by infiniteTransition.animateFloat(
    initialValue = 0.78f,
    targetValue = 1.14f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "sparkle_pulse",
  )
  val waveShift by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "wave_shift",
  )

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("empty_cart_cleaning_hero"),
    shape = RoundedCornerShape(24.dp),
    color = PureWhite,
    shadowElevation = 2.dp,
    border = BorderStroke(1.dp, BorderNeutral),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // المساحة البصرية المتحركة المستلهمة من شاشة البداية بروح المنظفات المنزلية
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(236.dp)
          .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
          .background(PureWhite),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width
          val h = size.height

          // هالة دافئة ومائية ناعمة في الخلفية
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                Color(0xFFE0F2FE).copy(alpha = 0.75f),
                Color(0xFFFFF9DB).copy(alpha = 0.55f),
                PureWhite,
              ),
              center = Offset(w * 0.5f, h * 0.44f),
              radius = w * 0.56f,
            ),
            radius = w * 0.56f,
            center = Offset(w * 0.5f, h * 0.44f),
          )

          // موجة علوية ركنية هادئة (مستوحاة من شاشة البداية)
          val topCornerWave = Path().apply {
            moveTo(w, 0f)
            lineTo(w * 0.55f, 0f)
            cubicTo(
              w * 0.68f, h * (0.14f + 0.03f * waveShift),
              w * 0.86f, h * 0.22f,
              w, h * 0.36f,
            )
            close()
          }
          drawPath(
            path = topCornerWave,
            brush = Brush.linearGradient(
              colors = listOf(PrimaryYellow.copy(alpha = 0.32f), Color(0xFFFFDF38).copy(alpha = 0.72f)),
              start = Offset(w * 0.55f, 0f),
              end = Offset(w, h * 0.36f),
            ),
          )

          // موجات سفلية متداخلة (الذهبية + الفحمية) بشكل مختلف عن شاشة البداية
          val yellowWave = Path().apply {
            moveTo(0f, h * (0.74f + 0.025f * waveShift))
            cubicTo(
              w * 0.30f, h * 0.62f,
              w * 0.66f, h * 0.90f,
              w, h * (0.72f - 0.02f * waveShift),
            )
            lineTo(w, h)
            lineTo(0f, h)
            close()
          }
          drawPath(
            path = yellowWave,
            brush = Brush.horizontalGradient(
              colors = listOf(Color(0xFFFFE55C), PrimaryYellow, PrimaryYellowDark),
            ),
          )

          val charcoalWave = Path().apply {
            moveTo(0f, h * (0.86f - 0.02f * waveShift))
            cubicTo(
              w * 0.35f, h * 0.96f,
              w * 0.68f, h * 0.76f,
              w, h * (0.85f + 0.02f * waveShift),
            )
            lineTo(w, h)
            lineTo(0f, h)
            close()
          }
          drawPath(
            path = charcoalWave,
            brush = Brush.verticalGradient(
              colors = listOf(CharcoalBlack, CharcoalDark),
              startY = h * 0.76f,
              endY = h,
            ),
          )

          // فقاعات صابون صاعدة مع لمعة قوسية
          drawRisingSoapBubbles(
            specs = CleaningBubbleSpecs,
            progress = bubbleProgress,
            maxHeightRatio = 0.88f,
          )

          // نجوم بريق النظافة النابضة
          drawFourPointSparkle(
            center = Offset(w * 0.22f, h * 0.30f),
            radius = 9.dp.toPx() * sparklePulse,
            color = PrimaryYellowDark,
          )
          drawFourPointSparkle(
            center = Offset(w * 0.80f, h * 0.28f),
            radius = 11.dp.toPx() * (1.92f - sparklePulse),
            color = AquaFresh,
          )
          drawFourPointSparkle(
            center = Offset(w * 0.72f, h * 0.60f),
            radius = 7.dp.toPx() * sparklePulse,
            color = PrimaryYellow,
          )
        }

        // الشارة المركزية العائمة (عربة + عبوة منظف وبريق)
        Box(
          modifier = Modifier
            .offset(y = (-6 + floatOffset).dp),
          contentAlignment = Alignment.Center,
        ) {
          // هالة دائرية ناعمة
          Surface(
            modifier = Modifier.size(116.dp),
            shape = CircleShape,
            color = PureWhite,
            shadowElevation = 10.dp,
            border = BorderStroke(2.5.dp, PrimaryYellow),
          ) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(
                  Brush.radialGradient(
                    colors = listOf(AquaFreshSoft.copy(alpha = 0.65f), PureWhite),
                  ),
                ),
              contentAlignment = Alignment.Center,
            ) {
              Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy((-6).dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.CleaningServices,
                  contentDescription = null,
                  tint = AquaFresh,
                  modifier = Modifier
                    .size(40.dp)
                    .rotate(-10f),
                )
                Icon(
                  imageVector = Icons.Filled.ShoppingCart,
                  contentDescription = null,
                  tint = CharcoalBlack,
                  modifier = Modifier.size(44.dp),
                )
              }
            }
          }

          // شارة نجمة البريق الصغيرة المتلألئة على زاوية الشعار
          Surface(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .offset(x = 6.dp, y = (-2).dp)
              .size(34.dp)
              .scale(sparklePulse),
            shape = CircleShape,
            color = PrimaryYellow,
            shadowElevation = 4.dp,
            border = BorderStroke(2.dp, PureWhite),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = CharcoalBlack,
                modifier = Modifier.size(18.dp),
              )
            }
          }

          // قطرة انتعاش في الجهة المقابلة
          Surface(
            modifier = Modifier
              .align(Alignment.BottomStart)
              .offset(x = (-6).dp, y = 4.dp)
              .size(30.dp),
            shape = CircleShape,
            color = CharcoalBlack,
            shadowElevation = 4.dp,
            border = BorderStroke(1.5.dp, PureWhite),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.WaterDrop,
                contentDescription = null,
                tint = PrimaryYellow,
                modifier = Modifier.size(16.dp),
              )
            }
          }
        }
      }

      // المحتوى النصي والأزرار
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 22.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "عربة منظفاتك فارغة حاليًا",
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            fontSize = 21.sp,
          ),
          color = TextPrimary,
          textAlign = TextAlign.Center,
        )

        // قوس ذهبي ناعم أسفل العنوان بلمسة شاشة البداية
        Canvas(
          modifier = Modifier
            .width(136.dp)
            .height(8.dp),
        ) {
          val swoosh = Path().apply {
            moveTo(0f, size.height * 0.7f)
            quadraticTo(size.width * 0.5f, -size.height * 0.35f, size.width, size.height * 0.7f)
          }
          drawPath(
            path = swoosh,
            brush = Brush.horizontalGradient(
              colors = listOf(PrimaryYellow.copy(alpha = 0.25f), PrimaryYellow, PrimaryYellowDark),
            ),
            style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round),
          )
        }

        Text(
          text = "املأ سلتك بأقوى المنظفات المنزلية ومعطرات الأسطح والغسيل بأسعار القطاعي والجملة وتوصيل سريع لباب بيتك أو محلك.",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
          textAlign = TextAlign.Center,
        )

        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          CleaningBenefitPill(icon = Icons.Filled.CleaningServices, label = "منظفات منزلية أصلية")
          CleaningBenefitPill(icon = Icons.Filled.LocalOffer, label = "عروض الجملة والقطاعي")
          CleaningBenefitPill(icon = Icons.Filled.LocalShipping, label = "توصيل سريع وآمن")
        }

        Spacer(modifier = Modifier.height(6.dp))

        AlEmanPrimaryButton(
          text = "تسوق المنظفات الآن",
          icon = Icons.Filled.ShoppingCart,
          onClick = onShopNow,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_cart_shop_now_button"),
        )
      }
    }
  }
}

/**
 * ترويسة متحركة لشاشة تسجيل الدخول وإنشاء الحساب:
 * تعكس هوية التطبيق كمتجر للمنظفات المنزلية بالقطاعي والجملة عبر فقاعات صابون وبريق متلألئ
 * وموجات العلامة التجارية وحركة طفو ناعمة وبسيطة.
 */
@Composable
fun SignInCleaningHeroHeader(
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "sign_in_cleaning_motion")
  val bubbleProgress by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 5800, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "sign_in_bubbles",
  )
  val badgeFloat by infiniteTransition.animateFloat(
    initialValue = -4.5f,
    targetValue = 4.5f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "sign_in_float",
  )
  val sparkleScale by infiniteTransition.animateFloat(
    initialValue = 0.82f,
    targetValue = 1.15f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "sign_in_sparkle",
  )

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("sign_in_animated_hero"),
    shape = RoundedCornerShape(AlEmanDimens.RadiusLg),
    color = PureWhite,
    shadowElevation = 3.dp,
    border = BorderStroke(1.dp, BorderNeutral),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(216.dp)
        .clip(RoundedCornerShape(AlEmanDimens.RadiusLg))
        .background(PureWhite),
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // هالة ناعمة تعكس الانتعاش والنظافة
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              Color(0xFFFFFCEB),
              Color(0xFFE0F2FE).copy(alpha = 0.48f),
              PureWhite,
            ),
            center = Offset(w * 0.5f, h * 0.42f),
            radius = w * 0.65f,
          ),
          radius = w * 0.65f,
          center = Offset(w * 0.5f, h * 0.42f),
        )

        // الموجة العلوية الهادئة باللون الأصفر
        val topWave = Path().apply {
          moveTo(0f, 0f)
          lineTo(w * 0.44f, 0f)
          cubicTo(w * 0.34f, h * 0.14f, w * 0.14f, h * 0.22f, 0f, h * 0.34f)
          close()
        }
        drawPath(
          path = topWave,
          brush = Brush.linearGradient(
            colors = listOf(Color(0xFFFFDF38), PrimaryYellow),
            start = Offset.Zero,
            end = Offset(w * 0.44f, h * 0.34f),
          ),
        )

        // الموجة السفلية الذهبية + الفحمية المتناغمة مع شاشة البداية
        val bottomYellow = Path().apply {
          moveTo(0f, h * 0.72f)
          cubicTo(w * 0.32f, h * 0.85f, w * 0.68f, h * 0.78f, w, h * 0.64f)
          lineTo(w, h)
          lineTo(0f, h)
          close()
        }
        drawPath(
          path = bottomYellow,
          brush = Brush.horizontalGradient(
            colors = listOf(Color(0xFFFFE140), PrimaryYellow, PrimaryYellowDark),
          ),
        )

        val bottomCharcoal = Path().apply {
          moveTo(0f, h * 0.84f)
          cubicTo(w * 0.28f, h * 0.76f, w * 0.68f, h * 0.90f, w, h * 0.73f)
          lineTo(w, h)
          lineTo(0f, h)
          close()
        }
        drawPath(
          path = bottomCharcoal,
          brush = Brush.verticalGradient(
            colors = listOf(CharcoalBlack, CharcoalDark),
            startY = h * 0.73f,
            endY = h,
          ),
        )

        drawRisingSoapBubbles(
          specs = CleaningBubbleSpecs,
          progress = bubbleProgress,
          maxHeightRatio = 0.85f,
        )

        drawFourPointSparkle(
          center = Offset(w * 0.16f, h * 0.46f),
          radius = 8.dp.toPx() * sparkleScale,
          color = AquaFresh,
        )
        drawFourPointSparkle(
          center = Offset(w * 0.84f, h * 0.30f),
          radius = 10.dp.toPx() * (1.95f - sparkleScale),
          color = PrimaryYellowDark,
        )
      }

      // شارة التطبيق العلوية
      BrandPillBadge(
        text = stringResource(id = R.string.brand_tagline),
        containerColor = PrimaryYellow,
        contentColor = CharcoalBlack,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(12.dp),
      )

      // المحور المركزي: شعار المتجر + أيقونات المنظفات المتأرجحة بنعومة
      Row(
        modifier = Modifier
          .align(Alignment.Center)
          .offset(y = (-10 + badgeFloat).dp)
          .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // أيقونة المنظفات اليسرى
        Surface(
          modifier = Modifier
            .size(48.dp)
            .offset(y = (-badgeFloat * 0.6f).dp),
          shape = CircleShape,
          color = AquaFreshSoft,
          border = BorderStroke(1.5.dp, PureWhite),
          shadowElevation = 4.dp,
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Filled.CleaningServices,
              contentDescription = null,
              tint = AquaFresh,
              modifier = Modifier.size(24.dp),
            )
          }
        }

        // الشعار الرسمي للمتجر
        Surface(
          modifier = Modifier.size(86.dp),
          shape = RoundedCornerShape(24.dp),
          color = PureWhite,
          shadowElevation = 10.dp,
          border = BorderStroke(2.dp, PrimaryYellow),
        ) {
          Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = stringResource(id = R.string.app_name),
            modifier = Modifier
              .fillMaxSize()
              .clip(RoundedCornerShape(22.dp)),
            contentScale = ContentScale.Crop,
          )
        }

        // أيقونة الانتعاش والبريق اليمنى
        Surface(
          modifier = Modifier
            .size(48.dp)
            .offset(y = (badgeFloat * 0.6f).dp)
            .scale(sparkleScale),
          shape = CircleShape,
          color = PrimaryYellowSoft,
          border = BorderStroke(1.5.dp, PureWhite),
          shadowElevation = 4.dp,
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Filled.AutoAwesome,
              contentDescription = null,
              tint = CharcoalBlack,
              modifier = Modifier.size(22.dp),
            )
          }
        }
      }

      // شريط الهوية السفلي داخل الموجة الفحمية
      Row(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Filled.WaterDrop,
          contentDescription = null,
          tint = PrimaryYellow,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "عالم المنظفات المنزلية • قطاعي وجملة",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = PureWhite,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}

@Composable
private fun CleaningBenefitPill(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
) {
  Surface(
    shape = RoundedCornerShape(50),
    color = PrimaryYellowSoft,
    border = BorderStroke(1.dp, PrimaryYellow.copy(alpha = 0.45f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = CharcoalBlack,
        modifier = Modifier.size(14.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = CharcoalBlack,
      )
    }
  }
}

private fun DrawScope.drawRisingSoapBubbles(
  specs: List<BubbleSpec>,
  progress: Float,
  maxHeightRatio: Float,
) {
  val w = size.width
  val h = size.height
  specs.forEach { spec ->
    val cycle = (progress * spec.speedMultiplier + spec.phaseOffset) % 1f
    val y = h * (maxHeightRatio - cycle * (maxHeightRatio + 0.08f))
    val swayPx = sin((cycle * 2f * PI).toFloat()) * spec.swayAmplitudeDp.dp.toPx()
    val x = w * spec.xFraction + swayPx
    val radiusPx = spec.baseRadiusDp.dp.toPx() * (0.85f + 0.25f * sin((cycle * PI).toFloat()))
    val alpha = when {
      cycle < 0.15f -> cycle / 0.15f
      cycle > 0.82f -> ((1f - cycle) / 0.18f).coerceAtLeast(0f)
      else -> 1f
    } * 0.55f

    drawCircle(
      color = spec.tint.copy(alpha = alpha * 0.22f),
      radius = radiusPx,
      center = Offset(x, y),
    )
    drawCircle(
      color = spec.tint.copy(alpha = alpha * 0.75f),
      radius = radiusPx,
      center = Offset(x, y),
      style = Stroke(width = 1.4.dp.toPx()),
    )
    // لمعة داخلية صغيرة على حافة الفقاعة
    drawCircle(
      color = Color.White.copy(alpha = alpha),
      radius = radiusPx * 0.22f,
      center = Offset(x - radiusPx * 0.34f, y - radiusPx * 0.34f),
    )
  }
}

private fun DrawScope.drawFourPointSparkle(
  center: Offset,
  radius: Float,
  color: Color,
) {
  val inner = radius * 0.28f
  val sparklePath = Path().apply {
    moveTo(center.x, center.y - radius)
    quadraticTo(center.x + inner * 0.4f, center.y - inner * 0.4f, center.x + radius, center.y)
    quadraticTo(center.x + inner * 0.4f, center.y + inner * 0.4f, center.x, center.y + radius)
    quadraticTo(center.x - inner * 0.4f, center.y + inner * 0.4f, center.x - radius, center.y)
    quadraticTo(center.x - inner * 0.4f, center.y - inner * 0.4f, center.x, center.y - radius)
    close()
  }
  drawPath(path = sparklePath, color = color.copy(alpha = 0.72f))
}
