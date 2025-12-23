package si.f5.luna3419.script.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.font.FontWeight

@Composable
fun PlayerScreen(viewModel: PlayerViewModel) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black)
            .clickable(interactionSource = interactionSource, indication = null) { viewModel.onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.requiredSize(1152.dp, 648.dp).clipToBounds().background(Color.Black)
        ) {
            Box(
                modifier = Modifier.requiredSize(1152.dp, 648.dp).clipToBounds().background(Color.Black)

            ) {
                val bgi = viewModel.currentBgi
                if (bgi != null) {
                    Image(
                        bitmap = bgi,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().zIndex(0f),
                        contentScale = ContentScale.Crop
                    )
                }

                viewModel.activeObjects.values.forEach { obj ->
                    if (obj.visible) {
                        val widthDp = if (obj.image != null) obj.image.width.dp else 100.dp
                        val heightDp = if (obj.image != null) obj.image.height.dp else 100.dp

                        val xOffset = obj.x.dp - (widthDp / 2f)
                        val yOffset = obj.y.dp - (heightDp / 2f)

                        if (obj.image != null) {
                            Image(
                                bitmap = obj.image,
                                contentDescription = obj.name,
                                contentScale = ContentScale.None,
                                alignment = Alignment.TopStart,
                                modifier = Modifier.offset(x = xOffset, y = yOffset)
                                    .requiredSize(width = widthDp, height = heightDp).zIndex(10f)
                                    .then(if (obj.blur > 0) Modifier.blur(obj.blur.dp) else Modifier).graphicsLayer {
                                        rotationZ = obj.rotation
                                        scaleX = obj.scaleX
                                        scaleY = obj.scaleY
                                        alpha = obj.alpha
                                    })
                        } else {
                            Box(
                                modifier = Modifier.offset(x = xOffset, y = yOffset)
                                    .requiredSize(width = widthDp, height = heightDp).zIndex(10f)
                                    .background(Color.Red.copy(alpha = 0.5f)).border(2.dp, Color.White)
                            ) {
                                Text(
                                    text = "MISSING: ${obj.name}",
                                    color = Color.White,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }

                if (viewModel.screenOverlayColor.alpha > 0f) {
                    Box(
                        modifier = Modifier.fillMaxSize().zIndex(100f).background(viewModel.screenOverlayColor)
                    )
                }

                if (viewModel.isMessageBoxVisible && !viewModel.isWaitingChoice) {
                    Box(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 50.dp).width(1080.dp)
                            .height(200.dp).zIndex(100f).background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.width(720.dp).align(Alignment.TopCenter)
                        ) {
                            Box(modifier = Modifier.height(40.dp).fillMaxWidth()) {
                                if (viewModel.nameText.isNotEmpty()) {
                                    Text(
                                        text = ScriptUtils.parseRichText(viewModel.nameText),
                                        color = Color(0xFFFFF100),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.CenterStart)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(5.dp))

                            Box(modifier = Modifier.height(120.dp).fillMaxWidth()) {
                                Text(
                                    text = ScriptUtils.parseRichText(viewModel.messageText),
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    lineHeight = 28.sp,
                                    modifier = Modifier.align(Alignment.TopStart)
                                )
                            }
                        }
                    }
                }

                if (viewModel.isWaitingChoice && viewModel.activeChoices.isNotEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().zIndex(150f).background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            viewModel.activeChoices.forEach { (text, next) ->
                                Surface(
                                    onClick = { viewModel.onChoiceSelected(next) },
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.width(600.dp).height(60.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text, fontSize = 20.sp, color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (viewModel.transitionAlpha > 0f) {
                    Box(
                        modifier = Modifier.fillMaxSize().zIndex(2000f)
                            .background(Color.Black.copy(alpha = viewModel.transitionAlpha))
                    )
                }
            }
        }
    }
}

